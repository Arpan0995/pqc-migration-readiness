# PQC Migration Readiness Report: jjwt-0.13.0

- Auditor version: `0.1.0-SNAPSHOT`
- Score model: `v0`
- Generated: 2026-07-09T05:30:00.201711Z
- Findings: 193 across 408 files

> Difficulty score **S** and effort **tier** are a heuristic estimate, not yet a validated prediction of migration effort (see `docs/research/03-difficulty-scoring-model.md` §8 for the estimation-vs-validation phasing). The naive baseline **B0** is a raw count of vulnerable call sites. Urgency **U** is a separate axis (harvest-now-decrypt-later risk), not part of the difficulty estimate.

## Module ranking

| Rank | Module | Tier | Score S | Urgency U | Baseline B0 | LOC |
|---:|---|---|---:|---:|---:|---:|
| 1 | `impl` | CRITICAL | 213.72 | 300.0 | 0 | 20389 |
| 2 | `api` | HIGH | 41.72 | 62.0 | 0 | 17937 |
| 3 | `tdjar` | MEDIUM | 13.2 | 24.0 | 0 | 375 |

## Module: `impl`

Effort tier **CRITICAL** (score 213.72), urgency 300.0, 0 vulnerable call sites, 20389 LOC.

| Site | Rule | Difficulty | Why it is expensive |
|---|---|---:|---|
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:47` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:56` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:59` | `FRAG-F4-RSAPrivateCrtKey` | 1.0 | Couples code to the concrete key type RSAPrivateCrtKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:66` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:75` | `FRAG-F4-RSAPrivateCrtKey` | 1.0 | Couples code to the concrete key type RSAPrivateCrtKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:76` | `FRAG-F4-RSAPrivateCrtKey` | 1.0 | Couples code to the concrete key type RSAPrivateCrtKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:74` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:86` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:90` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:94` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:92` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:90` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:85` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:85` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |
| `impl/src/main/java/io/jsonwebtoken/impl/security/RsaPrivateJwkFactory.java:106` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |

_135 more finding(s) not shown._

## Module: `api`

Effort tier **HIGH** (score 41.72), urgency 62.0, 0 vulnerable call sites, 17937 LOC.

| Site | Rule | Difficulty | Why it is expensive |
|---|---|---:|---|
| `api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java:424` | `FRAG-F4-RSAKey` | 1.0 | Couples code to the concrete key type RSAKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java:429` | `FRAG-F4-RSAKey` | 1.0 | Couples code to the concrete key type RSAKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java:429` | `FRAG-F4-RSAKey` | 1.0 | Couples code to the concrete key type RSAKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java:403` | `FRAG-F4-ECKey` | 1.0 | Couples code to the concrete key type ECKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java:408` | `FRAG-F4-ECKey` | 1.0 | Couples code to the concrete key type ECKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java:408` | `FRAG-F4-ECKey` | 1.0 | Couples code to the concrete key type ECKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java:568` | `FRAG-F4-ECKey` | 1.0 | Couples code to the concrete key type ECKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java:568` | `FRAG-F4-RSAKey` | 1.0 | Couples code to the concrete key type RSAKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java:594` | `FRAG-F4-RSAKey` | 1.0 | Couples code to the concrete key type RSAKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java:596` | `FRAG-F4-RSAKey` | 1.0 | Couples code to the concrete key type RSAKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java:596` | `FRAG-F4-RSAKey` | 1.0 | Couples code to the concrete key type RSAKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java:620` | `FRAG-F4-ECKey` | 1.0 | Couples code to the concrete key type ECKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java:620` | `FRAG-F4-ECKey` | 1.0 | Couples code to the concrete key type ECKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/security/DynamicJwkBuilder.java:96` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `api/src/main/java/io/jsonwebtoken/security/DynamicJwkBuilder.java:109` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |

_16 more finding(s) not shown._

## Module: `tdjar`

Effort tier **MEDIUM** (score 13.2), urgency 24.0, 0 vulnerable call sites, 375 LOC.

| Site | Rule | Difficulty | Why it is expensive |
|---|---|---:|---|
| `tdjar/src/test/java/io/jsonwebtoken/all/JavaReadmeTest.java:352` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `tdjar/src/test/java/io/jsonwebtoken/all/JavaReadmeTest.java:352` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `tdjar/src/test/java/io/jsonwebtoken/all/JavaReadmeTest.java:368` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `tdjar/src/test/java/io/jsonwebtoken/all/JavaReadmeTest.java:368` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `tdjar/src/test/java/io/jsonwebtoken/all/JavaReadmeTest.java:369` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |
| `tdjar/src/test/java/io/jsonwebtoken/all/JavaReadmeTest.java:369` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |
| `tdjar/src/test/java/io/jsonwebtoken/all/JavaReadmeTest.java:388` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `tdjar/src/test/java/io/jsonwebtoken/all/JavaReadmeTest.java:388` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `tdjar/src/test/java/io/jsonwebtoken/all/JavaReadmeTest.java:404` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `tdjar/src/test/java/io/jsonwebtoken/all/JavaReadmeTest.java:404` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `tdjar/src/test/java/io/jsonwebtoken/all/JavaReadmeTest.java:405` | `FRAG-F4-ECPrivateKey` | 1.0 | Couples code to the concrete key type ECPrivateKey; a migration propagates through every caller of this API. |
| `tdjar/src/test/java/io/jsonwebtoken/all/JavaReadmeTest.java:405` | `FRAG-F4-ECPrivateKey` | 1.0 | Couples code to the concrete key type ECPrivateKey; a migration propagates through every caller of this API. |

