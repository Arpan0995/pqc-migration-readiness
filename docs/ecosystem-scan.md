---
title: PQC readiness of the Java ecosystem
---

# PQC readiness of the Java ecosystem

A snapshot of how much a post-quantum migration would touch across 27 widely used open-source Java projects, produced by running this repository's auditor over each project's latest release. Scanned on 2026-09-09 with auditor 1.4.0 on JDK 21.

Read every figure here as a **Phase 1 estimate**: the effort ranges come from a declared planning heuristic layered on a pre-registered difficulty score, not a validated prediction. The point of publishing them is to be corrected. If a ranking looks wrong for a project you know, that is the single most useful thing you can tell us, in [the scan-results discussion](https://github.com/Arpan0995/pqc-migration-readiness/discussions/16).

## What the numbers say

- **25 of 27** projects show quantum-vulnerable asymmetric-crypto findings; **2** (Apache Shiro, Apache PDFBox) come back completely clean.
- For **4** projects the one-engineer estimate is **none**: the two clean ones, plus Micronaut and Dropwizard, whose only findings sit in test code.
- Effort tiers across the set: 7 CRITICAL, 7 HIGH, 5 MEDIUM, 4 LOW, 4 NONE.
- The dominant cost, in almost every project with findings, is **concrete key-type coupling**: code written against `RSAPublicKey`, `ECPrivateKey` and similar concrete types instead of `PublicKey` / `PrivateKey`. That API churn, not the algorithm swap, is what makes a migration expensive.

## The ranking

Ordered by estimated one-engineer effort, highest first.

| Project | Domain | Version | Files | Findings | Top tier | Est. effort |
|---|---|---|---:|---:|---|---|
| Apache MINA SSHD | SSH | `2.19.0` | 1,413 | 337 | CRITICAL | ~5–12 months |
| jjwt | JWT / JOSE | `0.13.0` | 408 | 193 | CRITICAL | ~3–9 months |
| WildFly Elytron | Security framework | `2.9.2` | 1,226 | 178 | CRITICAL | ~3–9 months |
| Keycloak | Identity / SSO | `26.7.3` | 8,142 | 185 | HIGH | ~3–8 months |
| Apache CXF | Web services | `4.2.3` | 7,607 | 159 | CRITICAL | ~3–7 months |
| Spring Security | Auth framework | `7.1.1` | 4,138 | 251 | CRITICAL | ~8–21 weeks |
| webauthn4j | WebAuthn / FIDO2 | `0.31.10` | 1,034 | 125 | CRITICAL | ~7–19 weeks |
| Quarkus | App framework | `3.39.2` | 22,057 | 80 | MEDIUM | ~7–19 weeks |
| Netty | Network / TLS | `4.2.17` | 3,412 | 53 | CRITICAL | ~5–13 weeks |
| Eclipse Californium | CoAP / DTLS, IoT | `4.0.0-M6` | 1,011 | 38 | HIGH | ~4–10 weeks |
| pac4j | Security engine | `6.5.8` | 871 | 63 | HIGH | ~4–9 weeks |
| Apache Tomcat | Servlet container | `11.0.25` | 2,814 | 14 | MEDIUM | ~4–9 weeks |
| Undertow | Web server | `2.4.1` | 925 | 14 | HIGH | ~3–9 weeks |
| Apache Knox | Gateway | `3.0.0` | 1,783 | 116 | HIGH | ~3–8 weeks |
| Apache ZooKeeper | Coordination | `3.9.6` | 918 | 12 | HIGH | ~2–6 weeks |
| Apache MINA | Network framework | `2.2.9` | 628 | 14 | MEDIUM | ~2–4 weeks |
| Eclipse Vert.x | Reactive toolkit | `5.1.7` | 1,245 | 23 | HIGH | ~1–3 weeks |
| Apache Kafka | Streaming | `4.3.1` | 5,793 | 14 | MEDIUM | ~1–3 weeks |
| Apache ActiveMQ | Messaging | `6.3.2` | 4,511 | 10 | LOW | ~7–15 days |
| Apache HttpClient | HTTP client | `5.6.4` | 890 | 5 | MEDIUM | ~7–15 days |
| Eclipse Jetty | Servlet container | `12.1.13` | 5,987 | 10 | LOW | ~4–8 days |
| Square Keywhiz | Secrets manager | `0.11.0` | 323 | 1 | LOW | ~4–7 days |
| OkHttp | HTTP client | `5.5.0` | 56 | 1 | LOW | ~4–7 days |
| Micronaut | App framework | `5.1.14` | 4,429 | 12 | NONE | none |
| Dropwizard | Web framework | `5.0.2` | 917 | 1 | NONE | none |
| Apache Shiro | Auth framework | `3.0.1` | 780 | 0 | NONE | none |
| Apache PDFBox | PDF / signing | `3.0.8` | 1,435 | 0 | NONE | none |

## How to read a row

Take **Apache Kafka** or **Keywhiz**: a low finding count and a short effort estimate means the project keeps its crypto surface small or delegates it. A high count like **jjwt** or **Spring Security** is usually not a warning about code quality; it is the size of the key-handling API a new algorithm has to be threaded through. A **zero** like **Shiro** or **PDFBox** means the project leaves asymmetric crypto to the JDK, Bouncy Castle and its integrations, which a Shiro committer [confirmed on our thread](https://github.com/apache/shiro/discussions/2886).

## What the auditor cannot see

Stated plainly, because it changes how to read the table:

- **Algorithms chosen through configuration or wrapper APIs are invisible.** The scan reads source, not runtime wiring, so a project that selects its algorithm from a properties file or through a framework API shows less than it really uses. A Quarkus maintainer [made exactly this point](https://github.com/quarkusio/quarkus/discussions/56506) about Vert.x, Netty and Elytron call sites. Eclipse Californium's maintainer [made the same point](https://github.com/eclipse-californium/californium/issues/2408): the project decides cipher-suite and group support at runtime through `CipherSuite.isSupported()` and `XECDHECryptography.SupportedGroup.isUsable()`, so the concrete-type sites the scan counts are gated by a capability check it cannot see.
- **Algorithm names routed through registries or enums are missed.** jjwt is the clearest case: it reports 0 vulnerable call sites because its signing flows through an algorithm registry, even though it uses RSA and ECDSA throughout. Tracked as [issue #5](https://github.com/Arpan0995/pqc-migration-readiness/issues/5).
- **The effort figures are heuristics.** They scale with the difficulty score; they are not measured migration time. Validating them against real migrations is the open research question.
- **Test code is separated, not counted in the plan.** Crypto in test source is deliberate.
- **Bouncy Castle, Tink and Conscrypt are excluded** on purpose: they implement crypto rather than consume it, so scanning them measures the wrong thing.

## Reproduce it

Every row is one command against a pinned checkout. Clone the project at the version in the table and:

```
./scan.sh path/to/project project-name
```

or download the auditor jar from Maven Central and point it at the source tree. Large projects need a larger heap (`java -Xmx12g -jar ...`); that is a known limitation, [issue #19](https://github.com/Arpan0995/pqc-migration-readiness/issues/19).

---

If you found PQC Migration Readiness interesting, please leave us a star on our GitHub repository ⭐ https://github.com/Arpan0995/pqc-migration-readiness
