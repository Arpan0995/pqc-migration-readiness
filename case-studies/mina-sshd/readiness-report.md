# PQC Migration Readiness Report: mina-sshd-sshd-2.13.1

- Auditor version: `0.1.0-SNAPSHOT`
- Score model: `v0`
- Generated: 2026-07-09T05:30:03.551745Z
- Findings: 323 across 1365 files

> Difficulty score **S** and effort **tier** are a heuristic estimate, not yet a validated prediction of migration effort (see `docs/research/03-difficulty-scoring-model.md` §8 for the estimation-vs-validation phasing). The naive baseline **B0** is a raw count of vulnerable call sites. Urgency **U** is a separate axis (harvest-now-decrypt-later risk), not part of the difficulty estimate.

## Module ranking

| Rank | Module | Tier | Score S | Urgency U | Baseline B0 | LOC |
|---:|---|---|---:|---:|---:|---:|
| 1 | `sshd-common` | CRITICAL | 369.03 | 504.0 | 0 | 62941 |
| 2 | `sshd-core` | MEDIUM | 25.61 | 40.0 | 7 | 70779 |
| 3 | `sshd-cli` | MEDIUM | 23.1 | 42.0 | 0 | 5659 |
| 4 | `sshd-openpgp` | MEDIUM | 20.85 | 36.0 | 0 | 1944 |
| 5 | `sshd-putty` | MEDIUM | 14.4 | 24.0 | 0 | 1255 |
| 6 | `sshd-contrib` | LOW | 7.7 | 14.0 | 0 | 3167 |

## Module: `sshd-common`

Effort tier **CRITICAL** (score 369.03), urgency 504.0, 0 vulnerable call sites, 62941 LOC.

| Site | Rule | Difficulty | Why it is expensive |
|---|---|---:|---|
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:862` | `FRAG-F4-DSAKey` | 1.0 | Couples code to the concrete key type DSAKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:864` | `FRAG-F4-RSAKey` | 1.0 | Couples code to the concrete key type RSAKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:866` | `FRAG-F4-ECKey` | 1.0 | Couples code to the concrete key type ECKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:867` | `FRAG-F4-ECKey` | 1.0 | Couples code to the concrete key type ECKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:867` | `FRAG-F4-ECKey` | 1.0 | Couples code to the concrete key type ECKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:1002` | `FRAG-F4-RSAKey` | 1.0 | Couples code to the concrete key type RSAKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:1005` | `FRAG-F4-DSAKey` | 1.0 | Couples code to the concrete key type DSAKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:1009` | `FRAG-F4-ECKey` | 1.0 | Couples code to the concrete key type ECKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:1010` | `FRAG-F4-ECKey` | 1.0 | Couples code to the concrete key type ECKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:1006` | `FRAG-F4-DSAKey` | 1.0 | Couples code to the concrete key type DSAKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:1003` | `FRAG-F4-RSAKey` | 1.0 | Couples code to the concrete key type RSAKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:1066` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:1066` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:1068` | `FRAG-F4-DSAPublicKey` | 1.0 | Couples code to the concrete key type DSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-common/src/main/java/org/apache/sshd/common/config/keys/KeyUtils.java:1068` | `FRAG-F4-DSAPublicKey` | 1.0 | Couples code to the concrete key type DSAPublicKey; a migration propagates through every caller of this API. |

_237 more finding(s) not shown._

## Module: `sshd-core`

Effort tier **MEDIUM** (score 25.61), urgency 40.0, 7 vulnerable call sites, 70779 LOC.

| Site | Rule | Difficulty | Why it is expensive |
|---|---|---:|---|
| `sshd-core/src/test/java/org/apache/sshd/client/ProxyTest.java:227` | `JCA-KPG-RSA` | 2.0 | Generates a quantum-vulnerable key pair (RSA); the algorithm choice must move to a PQC or hybrid scheme. |
| `sshd-core/src/test/java/org/apache/sshd/client/ProxyTest.java:287` | `JCA-KPG-RSA` | 2.0 | Generates a quantum-vulnerable key pair (RSA); the algorithm choice must move to a PQC or hybrid scheme. |
| `sshd-core/src/test/java/org/apache/sshd/client/ProxyTest.java:347` | `JCA-KPG-RSA` | 2.0 | Generates a quantum-vulnerable key pair (RSA); the algorithm choice must move to a PQC or hybrid scheme. |
| `sshd-core/src/test/java/org/apache/sshd/client/ProxyTest.java:413` | `JCA-KPG-RSA` | 2.0 | Generates a quantum-vulnerable key pair (RSA); the algorithm choice must move to a PQC or hybrid scheme. |
| `sshd-core/src/test/java/org/apache/sshd/client/auth/pubkey/InvalidRsaKeyAuthTest.java:57` | `JCA-KPG-EC` | 2.0 | Generates a quantum-vulnerable key pair (EC); the algorithm choice must move to a PQC or hybrid scheme. |
| `sshd-core/src/test/java/org/apache/sshd/client/auth/pubkey/InvalidRsaKeyAuthTest.java:90` | `JCA-KPG-RSA` | 2.0 | Generates a quantum-vulnerable key pair (RSA); the algorithm choice must move to a PQC or hybrid scheme. |
| `sshd-core/src/main/java/org/apache/sshd/common/kex/DHG.java:69` | `FRAG-F4-DHPublicKey` | 1.0 | Couples code to the concrete key type DHPublicKey; a migration propagates through every caller of this API. |
| `sshd-core/src/main/java/org/apache/sshd/common/kex/DHG.java:69` | `FRAG-F4-DHPublicKey` | 1.0 | Couples code to the concrete key type DHPublicKey; a migration propagates through every caller of this API. |
| `sshd-core/src/main/java/org/apache/sshd/common/kex/ECDH.java:77` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `sshd-core/src/main/java/org/apache/sshd/common/kex/ECDH.java:77` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `sshd-core/src/test/java/org/apache/sshd/common/global/OpenSshHostKeysHandlerTest.java:61` | `JCA-KPG-RSA` | 2.0 | Generates a quantum-vulnerable key pair (RSA); the algorithm choice must move to a PQC or hybrid scheme. |
| `sshd-core/src/test/java/org/apache/sshd/server/PublickeyAuthenticatorTest.java:66` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-core/src/test/java/org/apache/sshd/server/PublickeyAuthenticatorTest.java:66` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |

## Module: `sshd-cli`

Effort tier **MEDIUM** (score 23.1), urgency 42.0, 0 vulnerable call sites, 5659 LOC.

| Site | Rule | Difficulty | Why it is expensive |
|---|---|---:|---|
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:73` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:89` | `FRAG-F4-RSAPrivateCrtKey` | 1.0 | Couples code to the concrete key type RSAPrivateCrtKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:90` | `FRAG-F4-RSAPrivateCrtKey` | 1.0 | Couples code to the concrete key type RSAPrivateCrtKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:90` | `FRAG-F4-RSAPrivateCrtKey` | 1.0 | Couples code to the concrete key type RSAPrivateCrtKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:82` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:124` | `FRAG-F4-DSAPublicKey` | 1.0 | Couples code to the concrete key type DSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:132` | `FRAG-F4-DSAPrivateKey` | 1.0 | Couples code to the concrete key type DSAPrivateKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:180` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:188` | `FRAG-F4-ECPrivateKey` | 1.0 | Couples code to the concrete key type ECPrivateKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:259` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:264` | `FRAG-F4-DSAPublicKey` | 1.0 | Couples code to the concrete key type DSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:269` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:271` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:266` | `FRAG-F4-DSAPublicKey` | 1.0 | Couples code to the concrete key type DSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-cli/src/test/java/org/apache/sshd/cli/SshKeyDumpMain.java:261` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |

_6 more finding(s) not shown._

## Module: `sshd-openpgp`

Effort tier **MEDIUM** (score 20.85), urgency 36.0, 0 vulnerable call sites, 1944 LOC.

| Site | Rule | Difficulty | Why it is expensive |
|---|---|---:|---|
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPrivateKeyExtractor.java:79` | `FRAG-F4-DSAPublicKey` | 1.0 | Couples code to the concrete key type DSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPrivateKeyExtractor.java:75` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPrivateKeyExtractor.java:73` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPrivateKeyExtractor.java:98` | `FRAG-F4-ECPrivateKey` | 1.0 | Couples code to the concrete key type ECPrivateKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPrivateKeyExtractor.java:85` | `FRAG-F4-ECPrivateKey` | 1.0 | Couples code to the concrete key type ECPrivateKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPrivateKeyExtractor.java:86` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPrivateKeyExtractor.java:122` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPrivateKeyExtractor.java:115` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPrivateKeyExtractor.java:116` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPrivateKeyExtractor.java:146` | `FRAG-F4-DSAPrivateKey` | 1.0 | Couples code to the concrete key type DSAPrivateKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPrivateKeyExtractor.java:134` | `FRAG-F4-DSAPrivateKey` | 1.0 | Couples code to the concrete key type DSAPrivateKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPrivateKeyExtractor.java:135` | `FRAG-F4-DSAPublicKey` | 1.0 | Couples code to the concrete key type DSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPublicKeyExtractor.java:92` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPublicKeyExtractor.java:84` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-openpgp/src/main/java/org/apache/sshd/openpgp/PGPPublicKeyExtractor.java:142` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |

_3 more finding(s) not shown._

## Module: `sshd-putty`

Effort tier **MEDIUM** (score 14.4), urgency 24.0, 0 vulnerable call sites, 1255 LOC.

| Site | Rule | Difficulty | Why it is expensive |
|---|---|---:|---|
| `sshd-putty/src/main/java/org/apache/sshd/putty/DSSPuttyKeyDecoder.java:45` | `FRAG-F4-DSAPublicKey` | 1.0 | Couples code to the concrete key type DSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-putty/src/main/java/org/apache/sshd/putty/DSSPuttyKeyDecoder.java:45` | `FRAG-F4-DSAPrivateKey` | 1.0 | Couples code to the concrete key type DSAPrivateKey; a migration propagates through every caller of this API. |
| `sshd-putty/src/main/java/org/apache/sshd/putty/DSSPuttyKeyDecoder.java:49` | `FRAG-F4-DSAPublicKey` | 1.0 | Couples code to the concrete key type DSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-putty/src/main/java/org/apache/sshd/putty/DSSPuttyKeyDecoder.java:49` | `FRAG-F4-DSAPrivateKey` | 1.0 | Couples code to the concrete key type DSAPrivateKey; a migration propagates through every caller of this API. |
| `sshd-putty/src/main/java/org/apache/sshd/putty/ECDSAPuttyKeyDecoder.java:51` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `sshd-putty/src/main/java/org/apache/sshd/putty/ECDSAPuttyKeyDecoder.java:51` | `FRAG-F4-ECPrivateKey` | 1.0 | Couples code to the concrete key type ECPrivateKey; a migration propagates through every caller of this API. |
| `sshd-putty/src/main/java/org/apache/sshd/putty/ECDSAPuttyKeyDecoder.java:55` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `sshd-putty/src/main/java/org/apache/sshd/putty/ECDSAPuttyKeyDecoder.java:55` | `FRAG-F4-ECPrivateKey` | 1.0 | Couples code to the concrete key type ECPrivateKey; a migration propagates through every caller of this API. |
| `sshd-putty/src/main/java/org/apache/sshd/putty/RSAPuttyKeyDecoder.java:46` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-putty/src/main/java/org/apache/sshd/putty/RSAPuttyKeyDecoder.java:46` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |
| `sshd-putty/src/main/java/org/apache/sshd/putty/RSAPuttyKeyDecoder.java:50` | `FRAG-F4-RSAPublicKey` | 1.0 | Couples code to the concrete key type RSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-putty/src/main/java/org/apache/sshd/putty/RSAPuttyKeyDecoder.java:50` | `FRAG-F4-RSAPrivateKey` | 1.0 | Couples code to the concrete key type RSAPrivateKey; a migration propagates through every caller of this API. |

## Module: `sshd-contrib`

Effort tier **LOW** (score 7.7), urgency 14.0, 0 vulnerable call sites, 3167 LOC.

| Site | Rule | Difficulty | Why it is expensive |
|---|---|---:|---|
| `sshd-contrib/src/main/java/org/apache/sshd/contrib/common/signature/LegacyDSASigner.java:79` | `FRAG-F4-DSAKey` | 1.0 | Couples code to the concrete key type DSAKey; a migration propagates through every caller of this API. |
| `sshd-contrib/src/main/java/org/apache/sshd/contrib/common/signature/LegacyDSASigner.java:90` | `FRAG-F4-DSAPrivateKey` | 1.0 | Couples code to the concrete key type DSAPrivateKey; a migration propagates through every caller of this API. |
| `sshd-contrib/src/main/java/org/apache/sshd/contrib/common/signature/LegacyDSASigner.java:94` | `FRAG-F4-DSAPrivateKey` | 1.0 | Couples code to the concrete key type DSAPrivateKey; a migration propagates through every caller of this API. |
| `sshd-contrib/src/main/java/org/apache/sshd/contrib/common/signature/LegacyDSASigner.java:94` | `FRAG-F4-DSAPrivateKey` | 1.0 | Couples code to the concrete key type DSAPrivateKey; a migration propagates through every caller of this API. |
| `sshd-contrib/src/main/java/org/apache/sshd/contrib/common/signature/LegacyDSASigner.java:186` | `FRAG-F4-DSAPublicKey` | 1.0 | Couples code to the concrete key type DSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-contrib/src/main/java/org/apache/sshd/contrib/common/signature/LegacyDSASigner.java:190` | `FRAG-F4-DSAPublicKey` | 1.0 | Couples code to the concrete key type DSAPublicKey; a migration propagates through every caller of this API. |
| `sshd-contrib/src/main/java/org/apache/sshd/contrib/common/signature/LegacyDSASigner.java:190` | `FRAG-F4-DSAPublicKey` | 1.0 | Couples code to the concrete key type DSAPublicKey; a migration propagates through every caller of this API. |

