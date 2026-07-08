# 04 — Case-Study Plan

> **Phase 2 — deferred, not currently active.** The project is currently in
> Phase 1 (estimation only, no migration); this document describes the
> validation protocol for when Phase 2 starts. See
> [doc 03 §8](03-difficulty-scoring-model.md#8-project-phasing-estimation-now-validation-later).
> For what Phase 1 case-study selection actually looks like right now, see
> [`case-studies/README.md`](../../case-studies/README.md).

## 1. Two validation tracks

**Track A — controlled self-migrations.** We migrate 3–4 codebases to hybrid crypto ourselves, logging effort precisely (including wall-clock time). Full control, but single-rater realism limits.

**Track B — mined real-world migrations.** Some Java projects have *already* migrated: run the auditor on the pre-migration tag, then measure the actual historical migration diffs. No time data, but the effort data is ecologically real and produced by developers who never saw our scoring model — immune to our own biases.

Known Track B candidates (verified 2026-07-08):

| Project | Real migration evidence |
|---|---|
| Apache Mina SSHD | `sntrup761x25519` in 2.13.2; ML-KEM hybrids (`mlkem768x25519-sha256` etc.) in 3.0.0-M2 — auditor runs on the pre-PQC tag, effort = the actual kex-related commits between tags |
| Bouncy Castle (bctls) | Hybrid TLS named-group support commits |
| Others to confirm during pre-scan | Netty, Conscrypt (JNI-heavy — may not qualify), Tink Java |

## 2. Selection criteria (Track A)

1. Builds with Maven/Gradle on JDK 17/21 without exotic toolchains.
2. Has a real test suite — "build breakages" and "test failures" must be measurable.
3. **Direct** JCA/BC usage, not pure delegation to a TLS library (delegation-only codebases have near-zero migration surface and teach nothing).
4. Size spread: small (~10–50k LOC), medium (~50–200k), large (200k+, scoped to one module).
5. Domain spread: library / protocol implementation / framework / application.
6. License permits redistribution as a git submodule (pin exact commit for reproducibility).

## 3. Shortlist (pre-scan before committing; pick 4–5)

| Candidate | Domain | Size | Why it stresses the model |
|---|---|---|---|
| **jjwt** | JOSE/JWT library | small-med | Signature-centric; algorithm registry (F5 both ways); JWS size assumptions (F1); third-party alg enums (F8) |
| **Apache Shiro** (crypto/support modules) | Security framework | medium | Framework-style indirection; hashing vs signing mix; config-driven algorithm selection (F5 credit) |
| **Apache Mina SSHD** @ pre-PQC tag (~2.13.0) | SSH protocol | medium | Protocol negotiation (F3), wire-format lengths (F1/F2), host-key formats (F6) — and doubles as Track B ground truth |
| **Eclipse Californium / Scandium** | (D)TLS for IoT | medium | Handshake internals, cipher-suite enums (F3), record sizing under DTLS MTU pressure (F1 — PQC sizes hurt most here) |
| **pgpainless** | OpenPGP | small-med | Packet-format algorithm IDs (F2), persisted key rings (F6) |
| **Keycloak** (crypto SPI module only) or **Jenkins core** (scoped) | Enterprise app | large (scoped) | Realistic enterprise layering; keystores (F6); tests whether scoring survives scale |

## 4. Pre-scan protocol (before final selection)

Cheap grep-level count per candidate: hits on `KeyPairGenerator|KeyAgreement|Signature\.getInstance|Cipher\.getInstance|KeyStore\.getInstance` + concrete key interfaces. Keep candidates with **≥20 call sites**; record counts in `case-studies/pre-scan.md`. This uses only wave-1-level signals, so it cannot leak fragility structure into selection (bias control).

## 5. Track A migration protocol (per codebase)

1. Pin submodule at chosen commit; record JDK/build versions.
2. **Run auditor first**; commit the frozen report (timestamped) *before* migration starts.
3. Migrate to **hybrid** mode (classical + ML-KEM/ML-DSA via BC) — hybrid is the realistic 2026 enterprise target, per IETF/industry practice: key establishment → X25519+ML-KEM-768 pattern; signatures → dual-sign where format permits, else ML-DSA-65.
4. Work module-by-module; after each, log to `case-studies/<name>/effort-log.md`:
   - files touched, LOC added/deleted (from git diff)
   - build breakages (distinct compile-error batches)
   - test failures needing code fixes
   - wall-clock minutes (coarse, honest)
   - pain tags: `serialization | buffer-size | protocol-pinning | third-party-api | type-coupling | keystore | test-brittleness | other`
5. "Closely simulate" fallback (plan §4b): where a full working migration is impractical (e.g., third-party blocker), carry the change to compile-and-tests-adjusted state and tag the blocker — the blocker itself is data (F8 validation).

## 6. Structure

```
case-studies/
├── README.md            (protocol summary, links here)
├── pre-scan.md          (candidate counts, selection rationale)
└── <name>/              (one per selected codebase)
    ├── target/          (git submodule @ pinned commit)
    ├── audit-report.json / .md   (frozen BEFORE migration)
    └── effort-log.md    (Track A) or mined-diff-stats.md (Track B)
```

Submodules are added at selection time, not before (keeps the repo light until then).
