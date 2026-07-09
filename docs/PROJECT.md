# Project Overview & Current State

Authoritative status document for the **PQC Migration Readiness Framework**. The README
is the entry point; the detailed methodology lives in [`docs/research/`](research/). This
document records **what the project is, what has been built, and what remains** — updated
to reflect actual state (not the original design-phase plan).

_Last updated: 2026-07-09 (HEAD `c6140aa`)._

## 1. What this is

A research framework, in Java (JDK 21), with two linked components plus supporting modules:

- **`auditor`** — scans a Java codebase for quantum-vulnerable cryptographic usage and
  produces a per-module/file migration **difficulty score** and effort tier, with ranked
  hotspots and file:line references.
- **`agility-provider`** — a runtime abstraction over JCA/JCE that switches between
  classical, hybrid, and PQC-only algorithms by policy, with capability negotiation and
  audit logging.
- **`benchmarks`** — JMH harness measuring the agility layer's runtime cost across modes.
- **`case-studies`** — real public codebases (pinned submodules) scanned by the auditor.

## 2. Research question

> Can static code patterns in a Java codebase predict the effort required to migrate it to
> post-quantum cryptography?

The intended contribution is a **validated** scoring methodology; the tool is the
instrument. See §4 for how the project is currently phased against this.

## 3. Current state (what is built)

**Everything below is implemented, tested, committed, and pushed.** Tests: **77 passing**
(57 auditor across 8 classes, 20 agility-provider across 4), clean `mvn install` on JDK 21.

### Auditor (`auditor`, 24 source files)
- **Detection** (JavaParser, syntactic — no classpath required):
  - Waves 1–2: `Cipher`, `KeyPairGenerator`, `KeyFactory`, `KeyAgreement`, `Signature`
    entry points with the full quantum-vulnerable algorithm tables.
  - Wave 3 fragility indicators: **F1** (fixed-size buffers), **F3** (TLS/protocol
    pinning), **F4** (concrete key-type coupling), **F6** (persisted key material), merged
    onto co-located findings by enclosing scope.
  - **Constant propagation**: intra-file (local var / `final` field → literal) and
    cross-file (`Type.FIELD` resolved from a project-wide constant index built from the
    source tree, no classpath). HIGH confidence for literals, MEDIUM for resolved constants.
- **Scoring**: pre-registered model `v0` (weights frozen before any effort data), module
  aggregation with spread factor, naive baseline `B0`, qualitative **effort tier**
  (NONE/LOW/MEDIUM/HIGH/CRITICAL).
- **Reporting**: JSON (machine-readable) + Markdown (ranked hotspots with plain-language
  "why it's expensive") + CLI.

### Agility provider (`agility-provider`, 15 source files)
- **Negotiation core**: `Intent` × `Mode` model, `CryptoSuite` registry, policy-driven
  `Negotiator` (local-preference selection, fail-closed or floor-bounded downgrade),
  JSONL audit log.
- **BC-backed hybrid primitives**: `HybridKeyEstablishment` (ephemeral X25519 ECDH +
  ML-KEM-768, HKDF-combined) and `DualSignature` (ECDSA-P256 + ML-DSA-65, fail-closed AND
  verification). Delegates all primitives to Bouncy Castle; tests cover round-trips,
  tamper rejection, and FIPS size cross-checks.

### Benchmarks (`benchmarks`, 3 source files)
- JMH matrix over classical/hybrid/PQC for key establishment, signatures, and negotiation.
- **Campaign run** — results in [`benchmarks/results/RESULTS.md`](../benchmarks/results/RESULTS.md).

### Case studies
- Four pinned public codebases (jjwt, mina-sshd, californium, shiro) scanned; per-repo
  reports + cross-repo synthesis in [`case-studies/`](../case-studies/).

## 4. Project phasing (estimation now, validation later)

The original plan was a single validated study. It is now split:

- **Phase 1 — estimation (complete).** Run the auditor against real codebases; produce a
  score-derived **estimate** (a transparent heuristic, not a validated prediction), plus
  the agility-layer benchmark numbers. Everything in §3 and §5 is Phase 1.
- **Phase 2 — validation (deferred, not started).** Migrate (or mine) real codebases,
  measure effort, and correlate score vs. effort. Protocol preserved unchanged in
  [`docs/research/04`](research/04-case-study-plan.md) and [`05`](research/05-validation-and-benchmark-plan.md);
  `analysis/correlate.py` exists and is self-tested on synthetic data only.

Rationale: real migrations are a significant separate effort; the decision was to ship a
useful, honest estimation tool first and keep validation as a clean future phase.

## 5. Key results so far

**Auditor on 4 real codebases** (3,564 files; see [`case-studies/phase1-findings.md`](../case-studies/phase1-findings.md)):
- ~95% of findings are F4 concrete-key-type coupling; actual algorithm call sites are rare
  because mature libraries select algorithms via enums/constants/config, not literals.
- Constant propagation raised detected call sites **14 → 30** (californium 9→23,
  mina-sshd 5→7); jjwt (enum-based) and shiro (symmetric-only, a correct negative control)
  unchanged.
- Remaining detection frontier: enum/registry selection and runtime-dynamic `getInstance`.

**Agility-layer benchmarks** (Apple M2 / JDK 21.0.11; see [`benchmarks/results/RESULTS.md`](../benchmarks/results/RESULTS.md)):
- Negotiation overhead **~6–9 ns** — the agility layer's own cost is negligible.
- ML-KEM encaps/decaps are *faster* than X25519; ML-DSA signing is the expensive op
  (7.8× ECDSA).
- JVM-specific finding: hybrid/PQC allocate **4–13× more per op** — sustained GC pressure.

## 6. Deviations from the original design plan (honest record)

- **`bcpq-jdk18on` does not exist on Maven Central.** ML-KEM/ML-DSA/SLH-DSA ship in the
  main provider jar `org.bouncycastle:bcprov-jdk18on` (pinned 1.84). Verified by jar
  inspection.
- **Detection is syntactic, not symbol-solver-based.** JavaParser's symbol solver needs a
  compiled classpath we do not have for arbitrary case studies; a syntactic matcher runs
  on any source tree. Constant propagation was added later without needing a classpath.
- **Two-phase reframe** (estimation vs. validation), per §4.
- **Benchmarks must run from the module classpath, not the shaded jar** — BC ships ML-KEM
  in a multi-release jar the shade plugin flattens (see RESULTS.md). Caught because 6/20
  benchmarks silently failed on the first run.

## 7. What remains

**Phase 2 (deferred):** case-study migrations / mined migrations, effort logging,
correlation analysis, precision/recall against hand-labeled ground truth.

**Optional Phase-1 detection extensions (not planned unless requested):** F2 (fixed-width
persistence), F8 (third-party API boundary), JOSE/JWT surface, and — to reach the libraries
constant propagation can't (e.g. jjwt) — enum/registry modelling or dataflow for dynamic
algorithm selection.

## 8. Key facts / conventions

- **JDK 21 LTS**, Maven multi-module. Crypto via Bouncy Castle `bcprov-jdk18on:1.84`.
- Canonical remote: `github.com/Arpan0995/pqc-migration-readiness` (push only here).
- Case studies are pinned git submodules under `case-studies/<name>/repo` (`repo/`, not
  `target/`, which is gitignored); the repo stores pointers, not the external source.
- Primitive correctness is delegated to Bouncy Castle (KAT-tested upstream); our tests
  cover composition, not re-testing BC's primitives.
