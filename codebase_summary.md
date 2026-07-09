# Technical Summary — PQC Migration Readiness Framework

**Version analyzed:** v1.0.0 (tag `v1.0.0`) · **Repository:** github.com/Arpan0995/pqc-migration-readiness · **Date:** 2026-07-09

A Java (JDK 21) research framework for **estimating the cost of migrating a Java codebase to post-quantum cryptography (PQC) before the migration is attempted**. It couples a static-analysis *auditor* (detection + difficulty scoring + explained reports) with a runtime *crypto-agility layer* (policy-driven classical/hybrid/PQC-only suites with capability negotiation and audit logging), a completed JMH benchmark campaign, and estimation reports for four real open-source codebases (3,564 source files). All figures below are drawn from artifacts committed in this repository (test runs, `case-studies/*/readiness-report.json`, `case-studies/phase1-findings.md`, `benchmarks/results/jmh-results.json`, `benchmarks/results/jmh-run.log`).

---

## 1. Core Architecture and Data Flow

### 1.1 Module layout (multi-module Maven, JDK 21)

| Module | Main files | Role |
|---|---:|---|
| `auditor` | 24 | Static detection, scoring, report generation, CLI |
| `agility-provider` | 15 | Runtime agility: suites, policy, negotiation, hybrid primitives, JSONL audit |
| `benchmarks` | 3 | JMH overhead matrix (classical / hybrid / PQC-only) |
| `case-studies` | — | Four pinned public codebases (git submodules) + per-repo reports + synthesis |
| `analysis` | — | Phase-2 correlation harness (Python 3.9+, standard library only) |

### 1.2 Auditor data flow (two-pass pipeline)

```
Java source tree (no build, no classpath required)
        |
  [Pass 1]  JavaParser (Java-21 level) parses every .java file
        |     - unparseable files recorded, never fatal
        |     - non-blank LOC counted per file (size confounder control)
        |     - ConstantIndex built: every `static final String` with a
        |       literal (or literal-concatenation) initializer, project-wide
        v
  [Pass 2]  DetectionVisitor per compilation unit, with ScanContext
        |     - primary findings: vulnerable JCA usage, typed by Category
        |     - fragility signals: F1/F3/F6 observations
        |     - both tagged with a lexical *scope key*
        |       (enclosing method/constructor, else type)
        v
  Scope-key merge: co-located fragility indicators attach to the
  crypto findings they qualify (categories opt in via
  Category.acceptsFragilityTags() to prevent double counting)
        v
  ScanResult { findings, fileLineCounts, unparseableFiles }
        v
  ScoringEngine (frozen ScoreModel v0) + ModuleResolver
  (module = nearest enclosing build descriptor; fallback top-level dir)
        v
  ReadinessReport --> JsonReportWriter (machine-readable)
                  --> MarkdownReportWriter (ranked hotspots + "why expensive")
                  --> AuditorCli (entry point)
```

### 1.3 Agility-provider data flow

```
IntentPolicy (ordered suite prefs, fail-closed | downgrade w/ Mode floor)
      +  peer CapabilityDescriptor (ordered suite IDs per Intent)
        v
  Negotiator --> NegotiationResult { CryptoSuite, downgraded? }
        v
  HybridKeyEstablishment / DualSignature (primitives delegated to
  Bouncy Castle; only composition logic is original)
        v
  length-prefixed wire blobs  +  32-byte combined secrets
        v
  AuditLog (JSONL: ts, intent, mode, suite, peerOffer, outcome, duration)
```

One deliberate architectural invariant: **suite identifiers are a single vocabulary** shared by policy, negotiation offers, results, and audit records, so audit trails are directly interpretable against policy. A second: **detection and scoring are decoupled** — the scoring engine consumes findings and knows nothing about how they were produced, so the scoring methodology (the research contribution) evolves and is tested independently of the scanner.

---

## 2. Problem Solved and Methodology

### 2.1 The problem

NIST finalized ML-KEM (FIPS 203), ML-DSA (FIPS 204), and SLH-DSA (FIPS 205) in August 2024; NIST IR 8547 deprecates RSA/ECC-family algorithms after 2030 and disallows them after 2035. Organizations know *what* to migrate to. **No existing tool estimates what a migration will cost for a specific codebase**: CBOM inventory tools (CBOMkit / PQCA Sonar Cryptography plugin) list crypto assets, and misuse detectors (CogniCrypt, CryptoGuard) flag incorrect usage — neither predicts effort. Simultaneously, PQC performance literature targets C/C++/Rust almost exclusively, while enterprise Java fleets (JDK 8–21, common in banking/insurance/government) will not receive JDK-native PQC (JDK 24+; hybrid TLS in JDK 27) for years. The framework addresses both gaps: **effort estimation** for the **JVM ecosystem**.

### 2.2 Research question and hypothesis

> Can static code patterns in a Java codebase predict the actual effort required to migrate it to PQC?

**H1:** a difficulty score combining (a) crypto-API usage and (b) *structural fragility indicators* correlates with measured migration effort better than the naive baseline (a raw count of vulnerable call sites, `B0` — what any inventory tool effectively produces).

### 2.3 Methodology

- **Two-phase design.** Phase 1 (complete, v1.0.0): build the instrument, run it on real codebases, publish score-derived *estimates* explicitly framed as heuristics. Phase 2 (deferred, protocol committed): validate against *measured* effort via performed or mined migrations, using Spearman/Kendall correlation and a paired-bootstrap CI on delta-rho versus `B0`, with LOC-controlled partial correlations. The statistical harness exists and is self-tested on labeled synthetic data; it refuses to run on fewer than three modules of real data and never fabricates effort numbers.
- **Pre-registration as bias control.** Scoring weights (ScoreModel v0) were frozen in git *before* any effort ground truth exists; a tuned v1 may only be evaluated on codebases not used for tuning.
- **Case-study protocol.** Four public codebases pinned as git submodules at exact tags, selected on coarse criteria only (domain diversity, direct JCA use) so selection cannot leak the fragility structure the score keys on. Apache Shiro was included as a **designed negative control** (expected near-zero asymmetric crypto). Mina SSHD was pinned deliberately *pre*-PQC (2.13.1) — upstream added ML-KEM hybrids later — preserving a clean Phase-2 mining candidate.
- **Honest-output discipline.** Reports emit qualitative effort tiers, never person-day figures (no validated conversion exists); every report carries a banner that Phase-1 scores are estimates, not validated predictions.
- **Testing philosophy: composition, not primitives.** Bouncy Castle's algorithms are KAT-validated upstream; the 77 project tests (57 auditor, 20 agility-provider) exercise this project's own logic — full-pipeline scans on fixture trees, negative controls (symmetric crypto and PQC names never flagged; fragility signals must not leak across method scopes), the complete negotiation matrix, hybrid round-trips, tamper rejection, and FIPS-size cross-checks.

---

## 3. Key Algorithms, Mathematical Logic, and Data Structures

### 3.1 Classpath-free cross-file constant propagation (detection)

Mature libraries rarely write `getInstance("RSA")`; they write `getInstance(KeyUtils.RSA_ALGORITHM)`. Symbol solvers need a compiled classpath — unavailable when scanning arbitrary repositories. The auditor instead exploits holding the whole source tree:

- **`ConstantIndex`** (unique data structure): two maps built in pass 1 — `Type.FIELD -> literal value` (qualified lookups, always safe) and `bareName -> value` **only when the name maps to a single value project-wide** (ambiguity-safe static-import resolution; an ambiguous name never resolves arbitrarily).
- **`ConstantResolver`** (recursive, depth-capped at 8): resolves string literals; parenthesized/cast expressions; `+`-concatenations (constant iff both sides resolve); local variables (only if uniquely declared, initialized, and **never reassigned** in the enclosing callable); same-file `final` fields; then index lookups.
- **Confidence lattice:** `HIGH` for a direct literal; `MEDIUM` whenever any name/field indirection was followed — enabling precision-by-confidence calibration in Phase 2.

Measured effect (§4): detected real call sites across the four case studies rose **14 -> 30 (+114%)**, each recovered site manually spot-checked as a true positive.

### 3.2 Structural fragility indicators with scope-key co-location

The novel detection signal is *code shapes that make migration disproportionately expensive*, motivated by PQC artifact sizes (ML-DSA-65 signature 3,309 B vs. 64–72 B ECDSA; ML-KEM-768 ciphertext 1,088 B; public keys 1,184–2,592 B):

| ID | Indicator | Detection | Multiplier |
|---|---|---|---:|
| F1 | Fixed-size buffers at classical sentinel sizes {32, 64, 65, 70, 72, 91, 128, 256, 294, 384, 512} B | `new byte[C]`, `ByteBuffer.allocate(C)` | 1.5 |
| F3 | Protocol/suite pinning | legacy TLS version pins; `setEnabledCipherSuites/Protocols` | 1.5 |
| F4 | Concrete key-type coupling | `RSAPublicKey`/`ECPrivateKey`/... in type positions | 1.5 |
| F6 | Persisted key material | `KeyStore.getInstance`, `X509/PKCS8EncodedKeySpec` | 2.0 |
| F2 / F8 / F5 | fixed-width persistence / third-party boundary / config-sourced credit | specified; scoring-ready; not yet detected | 2.0 / 2.5 / 0.5 |

Indicators attach to crypto findings sharing a **lexical scope key** (enclosing method/constructor, else type) — a deliberately cheap proxy for dataflow adjacency that bounds false attachment to one method without full dataflow analysis. Structural findings themselves (`TYPE_COUPLING`, `TLS_CONFIG`) do not absorb co-located multipliers, preventing double counting.

### 3.3 Scoring model v0 (pre-registered mathematical logic)

Per finding *f* with category base weight `base(cat)` (key-establishment 3, signature 3, keygen 2, TLS-config 2, JOSE 2, type-coupling 1, informational 0):

```
d(f)      = base(cat) x min( PRODUCT of fragility multipliers , 6.0 )
S(module) = ( SUM of d(f) ) x spread ,  spread = 1 + 0.1 * log2(1 + filesWithFindings)
U(module) = SUM of base(cat) x w_urgency ,  w = 2.0 (confidentiality / HNDL) | 1.0 (signature)
B0        = count of scored non-type-coupling call sites   (naive baseline H1 must beat)
tier(S)   = NONE | LOW | MEDIUM | HIGH | CRITICAL  at thresholds 0 / 0.01 / 10 / 40 / 120
```

The logarithmic spread term encodes that forty findings in one file are a focused rewrite while the same count across 25 files is a campaign; **urgency is kept strictly orthogonal to difficulty** (harvest-now-decrypt-later makes confidentiality urgent *now*; signature forgery requires a future quantum computer) so Phase-2 validation of the difficulty claim stays uncontaminated.

### 3.4 Hybrid composition and negotiation (agility layer)

- **KEM combiner:** ephemeral X25519 ECDH and ML-KEM-768 encapsulation run independently; the shared secret is `HKDF-SHA256(ss_classical || ss_pqc)` (32 bytes, fixed info string) — the X25519MLKEM768 pattern; the result is secure if *either* component holds.
- **Dual signature:** ECDSA-P256 and ML-DSA-65 signatures over the same message, **fail-closed AND verification** (either component missing/corrupt fails the whole).
- **Wire format:** 4-byte big-endian length-prefixed block concatenation — deliberately trivial, since composite-signature encodings are still being standardized.
- **Negotiation algorithm:** first match over the *local* preference order intersected with the peer's offer (deterministic, O(|local| x |peer|)); on no intersection, policy selects fail-closed or the strongest peer suite whose `Mode` meets a floor (`CLASSICAL < HYBRID < PQC_ONLY`), with downgrades flagged for audit.

---

## 4. Performance Metrics, Benchmarks, and Experimental Results

### 4.1 Case-study results (from `case-studies/*/readiness-report.json`, synthesized in `phase1-findings.md`)

| Codebase (pinned) | Files | Findings | F4 type-coupling | Real call sites | via const-prop | Top tier |
|---|---:|---:|---:|---:|---:|---|
| jjwt 0.13.0 | 408 | 193 | 193 | 0 | 0 | CRITICAL |
| mina-sshd 2.13.1 | 1,365 | 323 | 316 | 7 | 2 | CRITICAL |
| californium 3.14.0 | 1,001 | 34 | 11 | 23 | 14 | MEDIUM |
| shiro 2.2.1 | 790 | 0 | 0 | 0 | 0 | — |
| **Total** | **3,564** | **550** | **520 (95%)** | **30** | **16** | |

**Finding A — coupling dominance.** 95% of everything the auditor finds in mature Java libraries is concrete key-type coupling, not algorithm call sites. jjwt rates CRITICAL with **zero** visible RSA/EC calls: its migration cost is API-surface churn (interfaces, casts, key serialization), not call-site swaps. Consequence: inventory-style counting — what existing tools produce — measures the *smallest* part of the visible migration surface, and migration budgets should center on key-typed API surface with call-site counts read as a lower bound.

**Finding B — constant propagation matters and its limits are precise.** Cross-file constant resolution more than doubled true call-site detection (14 -> 30). What remains invisible is characterized exactly: enum/registry-based selection (jjwt's `SignatureAlgorithm`) and runtime-dynamic `getInstance(param)` — the measured frontier of static crypto discovery without per-library modeling or dataflow.

**Finding C — the negative control behaved.** Shiro's zero was verified against source (its crypto is symmetric AES/Blowfish + password hashing, out of scope by design): the detector does not false-positive on symmetric cryptography.

### 4.2 Benchmark campaign (from `benchmarks/results/jmh-results.json` and `jmh-run.log`)

Environment: Apple M2 (8 cores), OpenJDK 21.0.11, Bouncy Castle 1.84; JMH 1.37, 2 forks, 5x1s warmup, 5x1s measurement, average-time mode, GC profiler; 20 configurations.

| Operation | Classical | Hybrid | PQC-only |
|---|--:|--:|--:|
| KEM keygen | 43.6 µs | 102.4 µs (2.3x) | 55.6 µs (1.3x) |
| KEM encapsulate | 108.0 µs | 138.2 µs (1.3x) | **36.1 µs (0.3x)** |
| KEM decapsulate | 62.3 µs | 101.2 µs (1.6x) | **42.4 µs (0.7x)** |
| Signature keygen | 66.9 µs | 187.7 µs (2.8x) | 120.4 µs (1.8x) |
| Sign | 65.6 µs | 355.9 µs (5.4x) | **513.7 µs (7.8x)** |
| Verify | 72.3 µs | 171.7 µs (2.4x) | 119.9 µs (1.7x) |

Allocation per operation (GC profiler): KEM keygen 3,720 B classical vs. **47,073 B hybrid (12.7x)** / 43,321 B PQC (11.6x); sign 52,633 B classical vs. 266,227 B dual (5.1x) / **341,908 B ML-DSA (6.5x)**; overall hybrid/PQC allocate **4–13x more per operation**. Negotiation: **6.44 ns** (hybrid-capable peer) / **8.98 ns** (classical-only, downgrade path), 24 B/op.

**Significance.**
1. **The agility abstraction is effectively free:** negotiation costs single-digit nanoseconds — about seven orders of magnitude below the crypto it selects — empirically retiring the standard "an agility layer is too slow" objection to crypto-agility architectures.
2. **PQC is not uniformly slower; it is differently shaped.** ML-KEM encapsulation/decapsulation are *faster* than X25519 (0.3x / 0.7x) with cost shifted to keygen, while ML-DSA signing is the one genuinely expensive operation (7.8x; verification only 1.5–1.7x). Workload shape — not a blanket slowdown — should drive migration planning.
3. **The JVM-specific result is allocation pressure** (4–13x per op, driven by multi-KB keys/ciphertexts/signatures): sustained GC load under handshake- or signing-heavy traffic, a managed-runtime effect invisible to the C/Rust-centric PQC benchmarking literature and the clearest candidate for a standalone empirical contribution.

A replication-relevant artifact from the logs: the first campaign silently lost 6 of 20 configurations (`NoSuchAlgorithmException: no such algorithm: ML-KEM for provider BC`) because Bouncy Castle ships ML-KEM in a multi-release jar (`META-INF/versions/9/`) that the Maven shade plugin flattens; JMH omits failed benchmarks from its JSON without failing the run. Detection required checking result counts against expectation; the fix is running from the module classpath. Both the failure mode and the corrected method are documented in `benchmarks/results/RESULTS.md`.

### 4.3 Verification metrics

- **77/77 unit-integration tests pass** (57 auditor across 8 classes; 20 agility-provider across 4), clean `mvn install` on JDK 21.
- FIPS artifact-size cross-checks asserted in tests: ML-KEM-768 ciphertext = 1,088 B; ML-DSA-65 signature = 3,309 B.
- Scope of claims: all Phase-1 numbers are estimates from a pre-registered heuristic plus single-machine microbenchmarks (read ratios, not microseconds). H1 remains open by design until Phase 2 supplies measured-effort ground truth; the framework is built so either outcome — validation or refutation — is publishable.

---

*Primary dependencies: JavaParser 3.27.0 (used purely syntactically), Bouncy Castle `bcprov-jdk18on` 1.84 (all primitives; no separate `bcpq` artifact exists), Jackson 2.18.2, JUnit BOM 6.1.1, JMH 1.37. A companion long-form summary with a different sectioning (including inputs/outputs and dependency rationale) is at `docs/CODEBASE-SUMMARY.md` / `.pdf`.*
