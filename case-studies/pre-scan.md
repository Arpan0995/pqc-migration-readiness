# Case-Study Selection & Pre-Scan

Phase 1 (estimation). This records which public codebases we scanned, why, and the
exact pinned commits — so every report under `case-studies/<name>/` is reproducible.

## Framing (read first)

These are **well-engineered, actively-maintained projects** using cryptography that is
entirely standard and secure *today*. Nothing here is a vulnerability disclosure. The
auditor looks **forward**: which code would need to change to migrate to post-quantum
algorithms ahead of the NIST 2030/2035 deprecation timeline. "Quantum-vulnerable" means
"relies on RSA/EC-family crypto that a future quantum computer would break" — it is not
a statement that the code is insecure now. Classical RSA/ECDSA/ECDH usage is exactly
what every one of these projects *should* be using in 2026.

## Selected codebases

| Name | Repo | Pinned tag | Domain | Why selected |
|---|---|---|---|---|
| jjwt | `jwtk/jjwt` | `0.13.0` | JWT/JOSE signing library | Signature-centric; heavy concrete-key-type API; small enough for a clean first report |
| mina-sshd | `apache/mina-sshd` | `sshd-2.13.1` | SSH protocol implementation | Protocol negotiation + wire formats (F1/F3 territory). **Pinned pre-PQC on purpose** — sntrup761 landed in 2.13.2, ML-KEM in 3.0.0-M2 — leaving a clean Phase-2 comparison open |
| californium | `eclipse-californium/californium` | `3.14.0` | DTLS for constrained IoT | Different maintainer/domain; DTLS handshake + cert handling; expected rich in fixed-buffer (F1) code |
| shiro | `apache/shiro` | `shiro-root-2.2.1` | Security / auth framework | Framework-style indirection (different code shape); enterprise-auth is the project's core target population |

Deliberately excluded: **Bouncy Castle itself** — it *implements* RSA/EC internally, so
scanning it flags algorithm implementations, not application *use* of crypto. The tool
targets consumers of the JCA, not providers.

## Selection method (bias control)

Selection used only coarse, wave-1-level signals (domain, size, "does it use JCA
directly") — never the fragility structure the scoring model keys on. This keeps
selection from biasing the results, matching the protocol in
[`docs/research/04-case-study-plan.md`](../docs/research/04-case-study-plan.md) §4. In
Phase 1 there is no migration and no effort measurement, so selection bias only affects
which codebases we describe, not any validated claim.

## Reproducing a scan

Each codebase is a pinned git submodule at `case-studies/<name>/repo`. To reproduce:

```
git submodule update --init case-studies/<name>/repo
mvn -pl auditor dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "auditor/target/classes:$(cat auditor/target/cp.txt)" \
    org.pqcreadiness.auditor.cli.AuditorCli case-studies/<name>/repo \
    --out case-studies/<name> --name <name>
```

Cross-repo analysis of the results is in
[`phase1-findings.md`](phase1-findings.md).
