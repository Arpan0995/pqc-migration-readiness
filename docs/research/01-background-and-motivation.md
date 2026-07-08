# 01 — Background & Motivation

Status of facts verified: **2026-07-08** (web-checked; this is a fast-moving area — re-verify before citing in a paper).

> **Project phasing note:** the research question and hypothesis below describe
> the project's full intended arc. The project currently runs **Phase 1
> (estimation) only** — Phase 2 (the actual validation against measured
> migration effort) is deferred, not started. See
> [doc 03 §8](03-difficulty-scoring-model.md#8-project-phasing-estimation-now-validation-later)
> for what's active now vs. deferred.

## 1. Threat model

- **Shor's algorithm** on a cryptographically relevant quantum computer (CRQC) breaks all deployed public-key cryptography based on integer factorization and discrete logs: **RSA, DSA, DH, ECDH, ECDSA, EdDSA/Ed25519/Ed448, X25519/X448, ECIES, ECMQV**. Every elliptic-curve algorithm is in scope — a common blind spot is assuming "modern" Ed25519/X25519 are safe; they are not.
- **Grover's algorithm** halves the effective security of symmetric primitives. AES-256 and SHA-384+ retain comfortable margins; AES-128 loses margin but is not considered urgently broken. The auditor treats symmetric/hash usage as *informational*, not *vulnerable* (v1 scope: asymmetric only).
- **Harvest-now-decrypt-later (HNDL)**: encrypted traffic and data captured today can be decrypted once a CRQC exists. This makes *confidentiality* uses (key establishment, encryption) urgent **now**, while *authenticity* uses (signatures) only fail once a CRQC exists at verification time. The auditor separates **difficulty** (how hard to migrate) from **urgency** (how soon it matters) — see doc 03.

## 2. Standards landscape (verified 2026-07-08)

| Standard | What | Status |
|---|---|---|
| FIPS 203 (ML-KEM) | Lattice KEM (Kyber) | **Final**, Aug 2024 |
| FIPS 204 (ML-DSA) | Lattice signature (Dilithium) | **Final**, Aug 2024 |
| FIPS 205 (SLH-DSA) | Hash-based signature (SPHINCS+) | **Final**, Aug 2024 |
| FIPS 206 (FN-DSA) | Lattice signature (Falcon) | Draft pending |
| HQC | Code-based backup KEM | Selected Mar 2025; draft FIPS expected 2026 (not confirmed published as of 2026-07-08), final ~2027 |
| NIST IR 8547 | Transition timeline | Final (2025): quantum-vulnerable algorithms **deprecated after 2030, disallowed after 2035** |
| NIST CSWP 39 | Crypto-agility strategies & practices | **Final, Dec 2025** — directly frames our agility-provider design |
| draft-ietf-tls-ecdhe-mlkem | Hybrid TLS 1.3 key agreement (X25519MLKEM768 et al.) | IETF draft; already deployed at scale (browsers, CDNs) |

CNSA 2.0 (NSA) pushes even earlier adoption for national-security systems. The 2030/2035 IR 8547 dates are the anchor for the "why now" argument.

## 3. The JVM ecosystem (verified 2026-07-08)

| JDK | PQC capability |
|---|---|
| 21 LTS (our target) | `javax.crypto.KEM` API exists; **no PQC algorithms** — BC required |
| 24 | ML-KEM (JEP 496) + ML-DSA (JEP 497) in the JCA |
| 25 LTS | First LTS carrying native ML-KEM/ML-DSA |
| 26 | `jarsigner` ML-DSA support |
| 27 (Sept 2026) | **JEP 527**: hybrid TLS 1.3 key exchange (X25519MLKEM768 default-enabled) |

Implications for us:

- The enterprise installed base (mostly 8/11/17/21) will not see native PQC for years — an application-level agility layer on **JDK 21 + Bouncy Castle** targets exactly the population that needs migration tooling.
- Provider choice is itself a migration variable: BC today, native JCA on 25+ tomorrow. The agility layer treats the **provider as a policy field** (doc 06).
- **Correction to the original plan (§7)**: the artifact `bcpq-jdk18on` does not exist on Maven Central. ML-KEM/ML-DSA/SLH-DSA ship inside the main provider jar **`org.bouncycastle:bcprov-jdk18on`** (we pin 1.84, verified by jar inspection). BC also bundles NIST KAT-derived test vectors usable for differential testing.
- liboqs has Java bindings (`liboqs-java`) but they are JNI-based and historically less maintained; BC remains the right choice (pure-Java, KATs included).

## 4. Prior art and the gap

| Tool / effort | What it does | What it does NOT do |
|---|---|---|
| CBOMkit / Sonar Cryptography Plugin (IBM → PQCA/Linux Foundation) | Static scan of Java (JCA + BC APIs) → **inventory** as a Cryptography Bill of Materials (CycloneDX CBOM) | No difficulty/effort model; output is a list, not a prediction |
| CogniCrypt, CryptoGuard (academia) | Detect crypto API **misuse** (bad IVs, ECB, weak params) | Not PQC-migration oriented; no cost model |
| NIST NCCoE SP 1800-38 series | Migration **practices**: discovery, inventory, planning | Descriptive guidance, not a validated quantitative instrument |
| PQC Migration Handbook (TNO/CWI/AIVD) | Qualitative migration strategy | Same |
| liboqs / PQClean / oqs benchmarks | C/C++/Rust primitive performance | Nothing JVM-specific; no codebase-level analysis |

**The gap (confirmed intact as of 2026-07)**: every existing tool stops at *inventory* or *misuse detection*. None predicts **how much effort** a migration will take for a specific codebase, and none is validated against measured migration effort. That prediction + validation is this project's contribution.

## 5. Research question, hypothesis, contribution

- **RQ**: Can static code patterns in a Java codebase predict the actual effort required to migrate it to post-quantum cryptography?
- **H1**: A score combining (a) crypto API usage patterns and (b) structural fragility indicators correlates with measured migration effort **better than a naive count of crypto call sites** (the baseline any linter gives you).
- **Contribution**: the validated scoring methodology (doc 03) and its evaluation (doc 05). The tool is the instrument, not the contribution.
- A **negative result is publishable**: if fragility signals add nothing over raw counts, that tells organizations static triage is insufficient and dynamic/manual assessment is required — still actionable.

## 6. A lucky break for validation

Apache Mina SSHD **already performed a real PQC migration**: `sntrup761x25519-sha512` landed in 2.13.2, and ML-KEM hybrids (`mlkem768x25519-sha256`, `mlkem768nistp256-sha256`, `mlkem1024nistp384-sha384`) in 3.0.0-M2. Its commit history is *genuine* migration-effort ground truth (files touched, LOC, iterations) produced by people who had never seen our scoring model. Doc 04 builds a second validation track on such mined migrations.
