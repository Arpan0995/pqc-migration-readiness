# 02 — Detection Rule Catalog

Input to `auditor` implementation (step 3). Rules are grouped in waves; **wave 1 is the §10.8 minimal scanner** from the project plan. Every rule yields findings with `file:line`, matched API, algorithm, confidence, and category.

## 1. Size table (drives fragility rules)

PQC artifacts are 1–3 orders of magnitude larger than classical ones. Any code that assumes classical sizes breaks.

| Algorithm | Public key | Signature / ciphertext | Shared secret |
|---|---|---|---|
| RSA-2048 | ~294 B (SPKI) | 256 B sig | — |
| ECDSA P-256 | 65 B raw / ~91 B SPKI | 64 B raw / 70–72 B DER sig | — |
| Ed25519 | 32 B | 64 B sig | — |
| X25519 | 32 B | — | 32 B |
| **ML-KEM-768** | **1,184 B** | **1,088 B ct** | 32 B |
| ML-KEM-512 / 1024 | 800 / 1,568 B | 768 / 1,568 B ct | 32 B |
| **ML-DSA-65** | **1,952 B** | **3,309 B sig** | — |
| ML-DSA-44 / 87 | 1,312 / 2,592 B | 2,420 / 4,627 B sig | — |
| SLH-DSA (all sets) | 32–64 B | **7,856–49,856 B sig** | — |
| Hybrid X25519MLKEM768 | 1,216 B keyshare | 1,120 B response | 32 B (combined) |

Sentinel constants for fragility detection: `32, 64, 65, 70, 72, 91, 128, 256, 294, 384, 512` appearing as buffer sizes / column widths / length checks near crypto dataflow.

## 2. Wave 1 — minimal scanner (per plan §10.8)

- `javax.crypto.Cipher.getInstance(<literal>)` where the algorithm token (before the first `/`) ∈ {`RSA`, `ECIES`}.
- `java.security.KeyPairGenerator.getInstance(<literal>)` where literal ∈ {`RSA`, `DH`, `EC`, `DSA`}.
- Literal-only in wave 1; JCA names are **case-insensitive** — match accordingly.
- Tests: positive/negative fixtures, transformation strings (`"RSA/ECB/OAEPWithSHA-256AndMGF1Padding"`), casing variants, non-vulnerable algorithms (`"AES"`, `"ML-KEM"`) as negatives.

## 3. Wave 2 — full JCA entry-point coverage

| API | Vulnerable algorithm strings (aliases included) |
|---|---|
| `KeyPairGenerator` / `KeyFactory` | `RSA`, `RSASSA-PSS`, `DSA`, `DH`/`DiffieHellman`, `EC`, `ECDSA`, `ECDH`, `X25519`, `X448`, `XDH`, `Ed25519`, `Ed448`, `EdDSA`, `ECMQV` |
| `Signature` | `SHA{1,224,256,384,512}with{RSA,ECDSA,DSA}` (+`andMGF1`, `inP1363Format` variants), `RSASSA-PSS`, `NONEwith{RSA,ECDSA,DSA}`, `Ed25519`, `Ed448`, `EdDSA` |
| `KeyAgreement` | `DH`/`DiffieHellman`, `ECDH`, `ECMQV`, `X25519`, `X448`, `XDH` |
| `Cipher` | token before first `/` ∈ {`RSA`, `ECIES`} |
| `javax.crypto.KEM` (JDK 21+) | `DHKEM` (EC-based → vulnerable) |
| `AlgorithmParameters` / spec classes | `RSAKeyGenParameterSpec`, `ECGenParameterSpec`, `DHParameterSpec`, `DSAParameterSpec`, `PSSParameterSpec`, `NamedParameterSpec.{X25519,X448,ED25519,ED448}`, BC `ECNamedCurveParameterSpec` |
| TLS surface | `setEnabledCipherSuites` with `TLS_ECDHE_*`/`TLS_RSA_*` literals; `jdk.tls.namedGroups` values; `SSLContext.getInstance("TLSv1.x")` (pinning signal, see F3) |
| JOSE/JWT (RFC 7518 `alg`) | `RS256/384/512`, `ES256/384/512`, `ES256K`, `PS256/384/512`, `EdDSA` (jjwt, auth0 java-jwt, nimbus-jose) — `HS*` is fine |
| Concrete key interfaces | `java.security.interfaces.{RSAPublicKey, RSAPrivateKey, RSAKey, ECPublicKey, ECPrivateKey, ECKey, DSAPublicKey, DSAPrivateKey, EdECPublicKey, EdECPrivateKey, XECPublicKey, XECPrivateKey}`, `javax.crypto.interfaces.{DHPublicKey, DHPrivateKey}` — in casts, `instanceof`, fields, and method signatures |
| Informational only (not "vulnerable") | `AES-128` key sizes, `SHA-1`, `3DES`, `MD5` — reported at INFO severity for completeness |

## 4. Wave 3 — structural fragility indicators (the novel signal)

| ID | Indicator | Detection sketch | Why it predicts effort |
|---|---|---|---|
| **F1** | Fixed-size buffers near crypto dataflow | `new byte[C]`, `ByteBuffer.allocate(C)`, `Arrays.copyOf(x, C)` with C ∈ sentinel set, in same method as (or dataflow-adjacent to) a Key/Signature/Cipher value | ML-DSA sig (3.3 KB) will not fit a 256 B buffer; every such site is a code change + retest |
| **F2** | Fixed-width persistence / wire formats | JPA `@Column(length=C)` / `columnDefinition="VARBINARY(C)"` on key/sig/cert fields; `DataOutputStream` length-prefixed writes with constant lengths; fixed-offset parsing | Schema migrations and wire-format changes are the most expensive class of change (coordination beyond one codebase) |
| **F3** | Protocol/version/suite pinning | Literal arrays to `setEnabledCipherSuites`/`setEnabledProtocols`; hardcoded `"TLSv1.2"`; custom protocol magic/version constants adjacent to crypto | Pinning forecloses negotiation paths hybrid migration depends on |
| **F4** | Concrete-type coupling | Public API (params/returns/fields) typed as `RSAPublicKey`/`ECPrivateKey`/etc. instead of `PublicKey`/`Key`; casts/`instanceof` to them | Type coupling propagates a change through every caller; interface-typed code migrates in place |
| **F5** | Algorithm-name provenance (agility credit, inverse) | `getInstance(arg)`: literal or static-final-resolved literal = rigid; sourced from config/properties/registry = agile | Config-driven algorithm choice is exactly the crypto-agility CSWP 39 asks for; such sites migrate by config change |
| **F6** | Persisted key material formats | `KeyStore.getInstance("JKS"/"PKCS12")` + cert chains; `X509EncodedKeySpec`/`PKCS8EncodedKeySpec` over stored bytes; hand-rolled PEM/DER parsing; `Serializable` classes with `Key` fields | Stored material must be re-issued/re-encoded; data migration on top of code migration |
| **F7** | Urgency class (separate axis) | Category of the call: key-establishment/encryption → HNDL-urgent; signature → CRQC-contingent | Orders the migration plan; kept out of the difficulty score to keep validation clean |
| **F8** | Third-party API boundary | Vulnerable algorithm identifiers/keys crossing into external library calls we don't control (JWT builder alg enums, HSM/PKCS#11 mechanisms, cloud KMS SDK algorithm ids) | Migration is blocked on an upstream release — schedule risk, not just code effort |

## 5. Implementation notes (JavaParser)

- **Detection is syntactic, by design** (decided during implementation, deviating from the earlier "symbol resolution from wave 2" plan). Entry points are matched by the receiver's trailing type name. Rationale: JavaParser's symbol solver needs the target's **full compiled classpath**, which we will not have when scanning arbitrary case-study codebases we do not build. A purely syntactic matcher runs on any source tree and never aborts a scan.
- **Constant propagation is implemented** (`ConstantResolver` + `ConstantIndex`), both intra-file (a local variable or `final` field initialised to a literal) and cross-file (a `static final String` referenced as `Type.FIELD`, resolved from a project-wide constant table the scanner builds in a first pass from the source itself — still no compiled classpath). Confidence: `HIGH` for a direct string literal, `MEDIUM` when any variable/field indirection was followed, `MEDIUM` for type-coupling sites. This measurably matters on real code: across the four Phase-1 case studies it raised detected algorithm-selection call sites from 14 to 30 (see [`case-studies/phase1-findings.md`](../../case-studies/phase1-findings.md)).
- **Still not resolved** (the detection frontier, empirically identified in Phase 1): enum/registry-based algorithm selection (e.g. a library's `SignatureAlgorithm.RS256` — library semantics, not constant propagation) and runtime-dynamic selection (`getInstance(methodParam)`, `getInstance(obj.getTransformation())`). These need per-library modelling or dataflow, and are an accuracy trade-off to measure in the precision/recall evaluation (doc 05 §1), not a silent gap.
- Source-level scanning confirmed as the right default (readable file:line for reports); ASM fallback remains deferred until a case study lacks source.
- Every rule carries a machine-readable ID (`JCA-KPG-RSA`, `JCA-SIG-ECDSA`, `FRAG-F4-RSAPublicKey`, …) — the report and the ground-truth labels join on these IDs (doc 05).
- **Implemented so far**: wave 1 (Cipher, KeyPairGenerator); wave 2 (KeyFactory, KeyAgreement, Signature, plus F4 concrete key-type coupling); wave 3 fragility indicators **F1** (fixed-size `byte[]` / `ByteBuffer.allocate` at sentinel sizes), **F3** (legacy TLS-version pins via `SSLContext.getInstance`, and `setEnabledCipherSuites`/`setEnabledProtocols` pinning — emitted as `TLS_CONFIG` findings), and **F6** (persisted key material via `KeyStore.getInstance` and `X509`/`PKCS8EncodedKeySpec`). Fragility indicators are attached to co-located crypto findings via an enclosing-scope merge (method/constructor, else type). Still pending: **F2** (fixed-width persistence/wire formats), **F8** (third-party API boundary), the JOSE/JWT surface, and `LOW`-confidence constant propagation / optional symbol resolution. The scoring engine already applies every multiplier as soon as a finding carries the indicator.
