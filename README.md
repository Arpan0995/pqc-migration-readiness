# PQC Migration Readiness Framework

[![auditor on Maven Central](https://img.shields.io/maven-central/v/io.github.arpan0995/pqc-readiness-auditor.svg?label=auditor%20%E2%80%94%20Maven%20Central)](https://central.sonatype.com/artifact/io.github.arpan0995/pqc-readiness-auditor)
[![agility-provider on Maven Central](https://img.shields.io/maven-central/v/io.github.arpan0995/pqc-readiness-agility.svg?label=agility-provider%20%E2%80%94%20Maven%20Central)](https://central.sonatype.com/artifact/io.github.arpan0995/pqc-readiness-agility)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![JDK 21](https://img.shields.io/badge/JDK-21-orange.svg)](https://openjdk.org/projects/jdk/21/)

A research framework for answering a question current post-quantum-cryptography
guidance leaves open: **not *what* to migrate to, but *how much* a migration
will cost for a specific Java codebase — and where the expensive parts hide.**

## Install / Run

**Run the auditor on any Java source tree in one command** — no build, no
classpath wrangling. Download the self-contained CLI jar from Maven Central and
point it at a directory:

```bash
# Download the executable auditor (fat jar) from Maven Central
curl -L -o pqc-readiness-auditor.jar \
  https://repo1.maven.org/maven2/io/github/arpan0995/pqc-readiness-auditor/1.0.0/pqc-readiness-auditor-1.0.0-all.jar

# Scan a codebase; writes JSON + Markdown reports to ./audit-out
java -jar pqc-readiness-auditor.jar /path/to/java/project --out audit-out --name my-project
```

Output: `audit-out/readiness-report.json` (machine-readable) and
`audit-out/readiness-report.md` (ranked hotspots with file:line and *why each is
expensive*).

**Use the libraries in a Maven build:**

```xml
<!-- Static readiness auditor (scanner + pre-registered scoring model) -->
<dependency>
  <groupId>io.github.arpan0995</groupId>
  <artifactId>pqc-readiness-auditor</artifactId>
  <version>1.0.0</version>
</dependency>

<!-- Runtime crypto-agility layer (classical / hybrid / PQC-only) -->
<dependency>
  <groupId>io.github.arpan0995</groupId>
  <artifactId>pqc-readiness-agility</artifactId>
  <version>1.0.0</version>
</dependency>
```

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
  effects (GC/allocation pressure from multi-KB PQC artifacts) — **done**, see
  [`benchmarks/results/RESULTS.md`](benchmarks/results/RESULTS.md).

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

[`docs/PROJECT.md`](docs/PROJECT.md) is the authoritative project overview and current-state
document (what's built, key results, deviations from the original plan, what remains).

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

## Run the auditor (from source)

The published `-all` jar (see [Install / Run](#install--run)) is the easiest way
to run the auditor. To run it from a local build instead:

```
mvn -pl auditor -am package
java -jar auditor/target/pqc-readiness-auditor-1.0.0-all.jar <source-root> --out audit-out --name <label>
```

Produces `audit-out/readiness-report.json` (machine-readable, feeds the analysis
harness) and `audit-out/readiness-report.md` (ranked hotspots with reasons).

## Analyse results

Correlation harness (pure-stdlib Python) lives in [`analysis/`](analysis/):

```
python3 analysis/correlate.py --selftest          # validate the statistics
python3 analysis/correlate.py --report <report.json> --effort <effort.csv>
```

## Agility-layer benchmark results

Full JMH campaign (Apple M2, JDK 21.0.11, BC 1.84, `@Fork(2)`/`@Warmup(5)`/
`@Measurement(5)`, `-prof gc`) across classical / hybrid / PQC postures. Full tables,
methodology, and raw data in [`benchmarks/results/RESULTS.md`](benchmarks/results/RESULTS.md).
Headlines:

- **Negotiation overhead is negligible** — capability negotiation costs **~6–9 ns** and
  24 B/op, ~7 orders of magnitude below the crypto it selects. The "abstraction layer is
  too slow" objection does not survive the data.
- **PQC is not uniformly slower** — ML-KEM encapsulate/decapsulate are *faster* than
  X25519 (0.3× / 0.7×); the cost moves to keygen. ML-DSA signing is the expensive
  operation (**7.8× ECDSA**; dual-sign 5.4×), while verification stays modest (1.5–1.7×).
- **The JVM-specific cost is allocation** — hybrid/PQC operations allocate **4–13× more
  per op** (hybrid KE keygen 47 KB/op vs 3.7 KB classical; ML-DSA sign 342 KB/op vs
  53 KB), i.e. sustained GC pressure the C/Rust-heavy PQC literature does not surface.

| Operation | Classical | Hybrid | PQC-only |
|---|--:|--:|--:|
| KEM keygen | 43.6 µs | 102.4 µs (2.3×) | 55.6 µs (1.3×) |
| KEM encapsulate | 108.0 µs | 138.2 µs (1.3×) | 36.1 µs (0.3×) |
| Signature sign | 65.6 µs | 355.9 µs (5.4×) | 513.7 µs (7.8×) |
| Signature verify | 72.3 µs | 171.7 µs (2.4×) | 119.9 µs (1.7×) |

> Read the ratios, not the absolute µs — microbenchmarks on one machine. Reproduce on
> target hardware before quoting figures. Must be run from the module classpath, not the
> shaded jar (BC's ML-KEM is a multi-release jar the shade plugin flattens — see RESULTS.md).

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
- [x] Auditor detection: intra- and cross-file constant propagation
      (`getInstance(Type.FIELD)` resolved without a classpath)
- [x] JMH benchmark **campaign run** across classical/hybrid/PQC — results and
      interpretation in [`benchmarks/results/RESULTS.md`](benchmarks/results/RESULTS.md)
      (negotiation overhead ~6–9 ns; PQC allocation 4–13× classical)
- [x] Auditor run against 4 real public codebases — Phase 1 estimation reports +
      synthesis in [`case-studies/`](case-studies/)
- [ ] Remaining detection (optional): F2 (fixed-width persistence), F8 (third-party
      API boundary), JOSE/JWT surface, enum/registry & dynamic algorithm selection

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

[Apache License 2.0](LICENSE).

## Citation

If you use this work, please cite it via [`CITATION.cff`](CITATION.cff) (also
surfaced by GitHub's "Cite this repository" button).
