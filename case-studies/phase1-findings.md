# Phase 1 Findings — Cross-Repo PQC Migration Readiness Estimation

Scanned **2026-07-09** with auditor `0.1.0-SNAPSHOT`, score model `v0`. Four public Java
codebases, pinned commits in [`pre-scan.md`](pre-scan.md). Raw reports are in each
`case-studies/<name>/readiness-report.{json,md}`.

> **This is an estimation exercise, not a security audit and not a vulnerability
> disclosure.** All four projects use standard, currently-secure cryptography. Findings
> describe *what would need to change for a future post-quantum migration*, not defects.
> Every number below is a Phase-1 heuristic estimate, not a validated prediction (see
> [doc 03 §8](../docs/research/03-difficulty-scoring-model.md)).

## Summary

| Codebase | Files | Findings | Type-coupling (F4) | Real call sites (B0) | Top tier |
|---|---:|---:|---:|---:|---|
| jjwt 0.13.0 | 408 | 193 | 193 | 0 | CRITICAL |
| mina-sshd 2.13.1 | 1,365 | 321 | 316 | 5 | CRITICAL |
| californium 3.14.0 | 1,001 | 20 | 11 | 9 | MEDIUM |
| shiro 2.2.1 | 790 | 0 | 0 | 0 | — |
| **Total** | **3,564** | **534** | **520 (97%)** | **14 (3%)** | |

## Headline finding

**Across 3,564 files of mature Java crypto code, actual quantum-vulnerable
*algorithm-selection call sites* are rare (14 total, ~3% of findings). The
overwhelming majority of findings — 520, ~97% — are F4 *concrete-key-type coupling*:
code typed against `RSAPublicKey`, `ECPublicKey`, `ECPrivateKey`, `DSAPublicKey`, etc.**

This is the most important Phase-1 result, and it has two honest sides:

1. **It is a real, useful migration signal.** Type coupling is a genuine migration cost:
   an API typed against `ECPublicKey` cannot transparently accept an ML-DSA key, so the
   change propagates through every caller. For these libraries, the *bulk* of migration
   effort really would be reworking key-typed API surface, not swapping a handful of
   `getInstance` calls. The tool is pointing at something true.

2. **It also exposes a detection limitation, exactly as documented.** The auditor's
   detection is intentionally syntactic and literal-only (see
   [doc 02 §5](../docs/research/02-detection-rule-catalog.md)): it flags
   `getInstance("RSA")` but not `getInstance(alg)` where `alg` is a constant, enum, or
   config value. Mature libraries almost always do the latter — jjwt selects algorithms
   through its `SignatureAlgorithm`/`KeyAlgorithm` enums, Shiro passes transformation
   strings held in variables. So the near-total absence of call-site findings is partly
   real (these libraries *are* well-abstracted) and partly a blind spot
   (constant-propagation / symbol resolution, deferred, would recover many of them).

**Consequence for reading the scores:** because scores are ~97% driven by type coupling,
a "CRITICAL" tier here means *"dense concrete-key-type API surface,"* not *"heavy active
use of RSA."* jjwt's `impl` module scoring 213 reflects ~118 key-typed sites, not 118
RSA calls (it has zero). This is stated plainly so the tier is not over-read.

## Per-codebase readiness profile

### jjwt 0.13.0 — HIGH type-coupling, zero visible call sites
193 findings, **all** F4. The `impl` module (CRITICAL, 20k LOC) is densely typed against
RSA/EC key interfaces because that is jjwt's public contract. Zero `getInstance` call
sites were visible because jjwt resolves algorithms through its own enums — a clean
design that also renders algorithm selection invisible to literal-only scanning. Honest
migration read: the effort here is **API-surface churn** (key types → interfaces/PQC),
which the tool captures, plus enum-internal algorithm wiring, which it does not yet see.

### mina-sshd 2.13.1 — largest surface, protocol-shaped
321 findings (316 F4 + 5 key-generation call sites) across 6 modules. Broadest key-type
coupling of the four, including DSA (SSH still carries legacy host-key types) — a genuine
extra migration axis. Only 5 literal call sites surfaced despite this being a protocol
implementation, again because algorithm names flow through SSH's negotiation constants
rather than literals. Pinned pre-PQC on purpose: upstream later added ML-KEM hybrids
(3.0.0-M2), so this codebase is the natural candidate if Phase 2 ever mines a real
migration.

### californium 3.14.0 — smallest but most *balanced* signal
Only 20 findings, but the **richest mix**: 11 F4 + 5 key-generation + 2 ECDSA signature
sites + 2 TLS suite-pinning (F3) sites, concentrated in a DER/ASN.1 utility and the
Scandium DTLS module. This is the one codebase that uses literal algorithm strings in a
few real places, so its (small) score is the most representative of *active* crypto
usage rather than type coupling alone.

### shiro 2.2.1 — correctly near-zero (a useful negative control)
**Zero findings, and that appears to be correct.** Shiro has 0 `Signature.getInstance`,
0 `KeyPairGenerator`, and 0 concrete asymmetric key types; its cryptography is
predominantly **symmetric** (AES/Blowfish) and password hashing, which are not
quantum-vulnerable in the asymmetric sense and are out of scope by design (doc 01). Its
6 `Cipher.getInstance` calls take a *variable* transformation, so they are both symmetric
and literal-invisible. Shiro validates that the detector does **not** false-positive on
symmetric crypto — an important calibration point. (Small caveat: if any variable-sourced
algorithm ever resolved to an asymmetric scheme, we would miss it; nothing in the source
suggests it does.)

## What this means

**For the tool.** Phase 1 gives strong empirical motivation for the deferred
constant-propagation / symbol-resolution work: on real libraries, literal-only detection
finds almost no algorithm-selection sites. The type-coupling signal (F4) is doing the
real work today and is worth trusting as an API-surface indicator; the call-site signal
is currently under-powered on well-abstracted code and should be read as a lower bound.

**For a migration planner.** For libraries like these, budget the migration around
**key-typed API surface** (interfaces, casts, serialization of key material), not around
counting `getInstance` calls. A codebase can have a large, real migration cost with zero
literal RSA/EC calls — jjwt is the clean example.

**For the scoring model.** A future revision might weight or report type-coupling
separately from active call sites, so a "type-coupling-only" CRITICAL is distinguishable
from an "active-usage" CRITICAL. Noted for score model `v1` (Phase 2), not changed now —
`v0` stays frozen.

## Caveats

- Syntactic, literal-only detection: call-site counts are lower bounds (see above).
- No ground truth here: precision/recall and score-vs-effort validation are Phase 2.
- Scores/tiers are heuristics; there is deliberately no engineer-days figure.
- Reports reflect the exact pinned commits in `pre-scan.md`; later releases differ (e.g.
  mina-sshd ≥ 2.13.2 and 3.0.0-M2 already add PQC).
