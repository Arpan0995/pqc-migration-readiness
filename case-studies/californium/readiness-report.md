# PQC Migration Readiness Report: californium-3.14.0

- Auditor version: `0.1.0-SNAPSHOT`
- Score model: `v0`
- Generated: 2026-07-09T05:08:32.957674Z
- Findings: 20 across 1001 files

> Difficulty score **S** and effort **tier** are a heuristic estimate, not yet a validated prediction of migration effort (see `docs/research/03-difficulty-scoring-model.md` §8 for the estimation-vs-validation phasing). The naive baseline **B0** is a raw count of vulnerable call sites. Urgency **U** is a separate axis (harvest-now-decrypt-later risk), not part of the difficulty estimate.

## Module ranking

| Rank | Module | Tier | Score S | Urgency U | Baseline B0 | LOC |
|---:|---|---|---:|---:|---:|---:|
| 1 | `element-connector` | MEDIUM | 19.72 | 26.0 | 4 | 35472 |
| 2 | `scandium-core` | MEDIUM | 13.2 | 22.0 | 3 | 60956 |
| 3 | `element-connector-tcp-netty` | LOW | 6.95 | 8.0 | 2 | 3356 |

## Module: `element-connector`

Effort tier **MEDIUM** (score 19.72), urgency 26.0, 4 vulnerable call sites, 35472 LOC.

| Site | Rule | Difficulty | Why it is expensive |
|---|---|---:|---|
| `element-connector/src/main/java/org/eclipse/californium/elements/util/Asn1DerDecoder.java:932` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `element-connector/src/main/java/org/eclipse/californium/elements/util/Asn1DerDecoder.java:919` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `element-connector/src/main/java/org/eclipse/californium/elements/util/Asn1DerDecoder.java:1061` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `element-connector/src/main/java/org/eclipse/californium/elements/util/Asn1DerDecoder.java:1118` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `element-connector/src/main/java/org/eclipse/californium/elements/util/Asn1DerDecoder.java:1118` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `element-connector/src/main/java/org/eclipse/californium/elements/util/JceProviderUtil.java:449` | `JCA-SIG-ECDSA` | 3.0 | Quantum-vulnerable signature (ECDSA); PQC signatures are much larger and may break size assumptions. |
| `element-connector/src/main/java/org/eclipse/californium/elements/util/JceProviderUtil.java:450` | `JCA-KPG-EC` | 2.0 | Generates a quantum-vulnerable key pair (EC); the algorithm choice must move to a PQC or hybrid scheme. |
| `element-connector/src/test/java/org/eclipse/californium/elements/util/Asn1DerDecoderTest.java:311` | `JCA-SIG-ECDSA` | 3.0 | Quantum-vulnerable signature (ECDSA); PQC signatures are much larger and may break size assumptions. |
| `element-connector/src/test/java/org/eclipse/californium/elements/util/Asn1DerDecoderTest.java:346` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `element-connector/src/test/java/org/eclipse/californium/elements/auth/RawPublicKeyIdentityTest.java:56` | `JCA-KPG-EC` | 2.0 | Generates a quantum-vulnerable key pair (EC); the algorithm choice must move to a PQC or hybrid scheme. |

## Module: `scandium-core`

Effort tier **MEDIUM** (score 13.2), urgency 22.0, 3 vulnerable call sites, 60956 LOC.

| Site | Rule | Difficulty | Why it is expensive |
|---|---|---:|---|
| `scandium-core/src/main/java/org/eclipse/californium/scandium/dtls/cipher/XECDHECryptography.java:424` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `scandium-core/src/main/java/org/eclipse/californium/scandium/dtls/cipher/XECDHECryptography.java:424` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `scandium-core/src/main/java/org/eclipse/californium/scandium/dtls/cipher/XECDHECryptography.java:561` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `scandium-core/src/main/java/org/eclipse/californium/scandium/dtls/cipher/XECDHECryptography.java:562` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `scandium-core/src/main/java/org/eclipse/californium/scandium/dtls/cipher/XECDHECryptography.java:595` | `FRAG-F4-ECPublicKey` | 1.0 | Couples code to the concrete key type ECPublicKey; a migration propagates through every caller of this API. |
| `scandium-core/src/test/java/org/eclipse/californium/scandium/dtls/x509/CertificateConfigurationHelperTest.java:287` | `JCA-KF-EC` | 2.0 | Generates a quantum-vulnerable key pair (EC); the algorithm choice must move to a PQC or hybrid scheme. |
| `scandium-core/src/test/java/org/eclipse/californium/scandium/dtls/x509/CertificateConfigurationHelperTest.java:288` | `JCA-KF-EC` | 2.0 | Generates a quantum-vulnerable key pair (EC); the algorithm choice must move to a PQC or hybrid scheme. |
| `scandium-core/src/test/java/org/eclipse/californium/scandium/auth/PrincipalSerializerTest.java:63` | `JCA-KPG-RSA` | 2.0 | Generates a quantum-vulnerable key pair (RSA); the algorithm choice must move to a PQC or hybrid scheme. |

## Module: `element-connector-tcp-netty`

Effort tier **LOW** (score 6.95), urgency 8.0, 2 vulnerable call sites, 3356 LOC.

| Site | Rule | Difficulty | Why it is expensive |
|---|---|---:|---|
| `element-connector-tcp-netty/src/main/java/org/eclipse/californium/elements/tcp/netty/TlsClientConnector.java:166` | `TLS-SUITES-PINNED` | 3.0 | Pins classical TLS parameters that a hybrid migration must renegotiate. Protocol/suite pinning forecloses the negotiation hybrid migration needs. |
| `element-connector-tcp-netty/src/main/java/org/eclipse/californium/elements/tcp/netty/TlsServerConnector.java:103` | `TLS-SUITES-PINNED` | 3.0 | Pins classical TLS parameters that a hybrid migration must renegotiate. Protocol/suite pinning forecloses the negotiation hybrid migration needs. |

