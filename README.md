# PQC Migration Readiness Framework

A research framework for answering a question current post-quantum-cryptography
guidance leaves open: **not *what* to migrate to, but *how much* a migration
will cost for a specific Java codebase — and where the expensive parts hide.**

## Why

NIST finalized ML-KEM (FIPS 203), ML-DSA (FIPS 204), and SLH-DSA (FIPS 205) in
August 2024; NIST IR 8547 deprecates quantum-vulnerable algorithms (RSA, ECDSA,
(EC)DH, EdDSA, …) after 2030 and disallows them after 2035. Organizations know
the destination. What no existing tool provides is a **validated estimate of
migration effort**: existing scanners (CBOM inventories, misuse detectors) stop
at *listing* crypto usage. Meanwhile almost all PQC tooling research targets
C/C++/Rust — the enterprise JVM ecosystem is nearly unstudied, even though
JDK-native PQC only began arriving in JDK 24+ and most production fleets sit on
JDK 8–21 for years to come.

## Research question

> Can static code patterns in a Java codebase predict the actual effort
> required to migrate it to post-quantum cryptography?

**Hypothesis (H1):** a difficulty score built from (a) crypto API usage
patterns and (b) *structural fragility indicators* — hardcoded key/signature
buffer sizes, fixed-width serialization, protocol pinning, concrete key-type
coupling — predicts measured migration effort **better than naively counting
crypto call sites**. The validated scoring methodology is the research
contribution; the tool is the instrument. A negative result is a result.

**Current phase:** the project runs in two phases (see
[doc 03 §8](docs/research/03-difficulty-scoring-model.md)). **Phase 1
(now)** runs the auditor against real public codebases and produces a
score-derived effort **estimate** — a transparent heuristic, not yet a
validated prediction. **Phase 2 (deferred)** performs the actual
correlation study against measured migration effort (doc 04/05, unchanged,
just not started). Read every score/tier in this repo today as Phase 1
output.

## How

1. **Detect** (`auditor`): JavaParser-based scan for quantum-vulnerable JCA/JCE
   usage, validated by precision/recall against hand-labeled ground truth.
2. **Score** (`auditor`): pre-registered difficulty model (weights frozen
   *before* effort data is collected) per module/file.
3. **Validate** *(Phase 2, deferred — not currently active)*: migrate real
   open-source codebases to hybrid crypto and measure actual effort (files
   touched, LOC, breakages, time) — plus a second track mining projects that
   already migrated for real (e.g., Apache Mina SSHD's ML-KEM hybrid
   adoption). Correlate predicted score vs. measured effort (Spearman ρ),
   against the naive-count baseline.
4. **Provide agility** (`agility-provider`): a policy-driven runtime layer over
   JCA/JCE for classical / hybrid / PQC-only modes with capability negotiation
   and audit logging, benchmarked (`benchmarks`, JMH) to answer the standard
   "abstraction layers are too slow" objection with data.

## Expected outputs

**Phase 1 (now):**
- Per-codebase **readiness report**: module scores, a qualitative **effort
  tier** (`NONE`/`LOW`/`MEDIUM`/`HIGH`/`CRITICAL`), and ranked hotspots with
  file:line and *why each is expensive* — an estimation aid, not a validated
  prediction (see [doc 03 §8](docs/research/03-difficulty-scoring-model.md)).
- **Benchmark tables**: agility-layer overhead across modes, incl. JVM-specific
  effects (GC/allocation pressure from multi-KB PQC artifacts).

**Phase 2 (deferred):**
- **Correlation figure**: predicted difficulty vs. measured migration effort.
- **Precision/recall table** for the detector, against hand-labeled ground truth.

## Modules

| Module | Role |
|---|---|
| `auditor` | Scanner + difficulty scoring |
| `agility-provider` | Runtime crypto-agility layer (BC-backed, JDK 21) |
| `benchmarks` | JMH harness for agility-layer overhead |
| `case-studies` | Pinned target codebases + migration effort logs |

## Research documentation

Design decisions and methodology live in [`docs/research/`](docs/research/):

1. [Background & motivation](docs/research/01-background-and-motivation.md) — threat model, verified standards status, the gap
2. [Detection rule catalog](docs/research/02-detection-rule-catalog.md) — what the auditor flags and why
3. [Difficulty scoring model](docs/research/03-difficulty-scoring-model.md) — pre-registered score v0
4. [Case-study plan](docs/research/04-case-study-plan.md) — selection criteria, migration & effort-logging protocol
5. [Validation & benchmark plan](docs/research/05-validation-and-benchmark-plan.md) — statistics, interpretation grid, JMH matrix
6. [Agility provider design](docs/research/06-agility-provider-design.md) — policy, negotiation, hybrid composition

## Requirements

- JDK 21, Maven 3.9+
- Crypto primitives via `org.bouncycastle:bcprov-jdk18on` (ML-KEM/ML-DSA/SLH-DSA
  are bundled in the main provider jar — there is no separate `bcpq` artifact)

## Build

```
mvn clean install
```

## Run the auditor

```
mvn -pl auditor dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "auditor/target/classes:$(cat auditor/target/cp.txt)" \
    org.pqcreadiness.auditor.cli.AuditorCli <source-root> --out audit-out --name <label>
```

Produces `audit-out/readiness-report.json` (machine-readable, feeds the analysis
harness) and `audit-out/readiness-report.md` (ranked hotspots with reasons).

## Analyse results

Correlation harness (pure-stdlib Python) lives in [`analysis/`](analysis/):

```
python3 analysis/correlate.py --selftest          # validate the statistics
python3 analysis/correlate.py --report <report.json> --effort <effort.csv>
```

## Status

**Phase 1 — estimation (current focus):**
- [x] Multi-module scaffolding, build-verified on JDK 21 (JMH runs end-to-end)
- [x] Research phase: standards verified (2026-07-08), detection catalog,
      pre-registered scoring model v0
- [x] `auditor` detection waves 1–3: JCA entry points (Cipher, KeyPairGenerator,
      KeyFactory, KeyAgreement, Signature) and structural fragility indicators
      F1 (fixed buffers), F3 (TLS/protocol pinning), F4 (type coupling), F6
      (persisted key material)
- [x] Scoring engine (score v0), qualitative effort tier, JSON + Markdown
      reports, CLI — build-verified end-to-end
- [x] `agility-provider`: negotiation core (suites, policy, capability
      negotiation, audit log) + BC-backed hybrid primitives (KEM combiner,
      dual signature), 20 tests
- [x] JMH benchmark matrix (key establishment, signatures, negotiation) —
      builds and registers; full campaign not yet run
- [ ] Remaining detection: F2 (fixed-width persistence), F8 (third-party API
      boundary), JOSE/JWT surface
- [ ] Run the auditor against selected real public codebases and publish
      Phase 1 estimation reports

**Phase 2 — validation (deferred, not started):**
- [ ] Case-study migrations (real or mined) and effort logging (`docs/research/04`)
- [ ] Correlation analysis: score vs. measured effort (`docs/research/05`) —
      `analysis/correlate.py` exists and is validated against synthetic data
      only; real case-study data is what's missing
- [ ] Precision/recall against hand-labeled ground truth

> Research prototype. Not production crypto software. The auditor's detection is
> syntactic (no classpath required); its accuracy is to be measured, not assumed.
> Every score/tier currently produced is a Phase 1 estimate, not a validated
> prediction.

## License

TBD.
