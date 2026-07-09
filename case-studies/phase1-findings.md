# Phase 1 Findings — Cross-Repo PQC Migration Readiness Estimation

Scanned **2026-07-09** with auditor `0.1.0-SNAPSHOT` (detection waves 1–3 **plus
intra- and cross-file constant propagation**), score model `v0`. Four public Java
codebases, pinned commits in [`pre-scan.md`](pre-scan.md). Raw reports are in each
`case-studies/<name>/readiness-report.{json,md}`.

> **This is an estimation exercise, not a security audit and not a vulnerability
> disclosure.** All four projects use standard, currently-secure cryptography. Findings
> describe *what would need to change for a future post-quantum migration*, not defects.
> Every number below is a Phase-1 heuristic estimate, not a validated prediction (see
> [doc 03 §8](../docs/research/03-difficulty-scoring-model.md)).

## Summary

| Codebase | Files | Findings | Type-coupling (F4) | Real call sites (B0) | of which via const-prop | Top tier |
|---|---:|---:|---:|---:|---:|---|
| jjwt 0.13.0 | 408 | 193 | 193 | 0 | 0 | CRITICAL |
| mina-sshd 2.13.1 | 1,365 | 323 | 316 | 7 | 2 | CRITICAL |
| californium 3.14.0 | 1,001 | 34 | 11 | 23 | 14 | MEDIUM |
| shiro 2.2.1 | 790 | 0 | 0 | 0 | 0 | — |
| **Total** | **3,564** | **550** | **520 (95%)** | **30** | **16** | |

## Headline findings

**1. Type coupling dominates.** Across 3,564 files, ~95% of findings (520) are F4
*concrete-key-type coupling*: code typed against `RSAPublicKey`, `ECPublicKey`,
`DSAPublicKey`, etc. This is a genuine migration cost — an API typed against
`ECPublicKey` cannot transparently accept an ML-DSA key, so the change propagates through
every caller — and for these libraries it is the *bulk* of the migration surface, not a
handful of `getInstance` swaps. A "CRITICAL" tier here therefore means *"dense
concrete-key-type API surface,"* not *"heavy active use of RSA."* jjwt's `impl` module
(213, CRITICAL) has ~118 key-typed sites and **zero** actual RSA calls; that is stated
plainly so the tier is not over-read.

**2. Constant propagation more than doubled the real call sites we can see (14 → 30).**
The initial literal-only pass found only 14 actual algorithm-selection call sites in
3,564 files, because mature libraries rarely write `getInstance("RSA")`. They write
`getInstance(KeyUtils.RSA_ALGORITHM)` or `getInstance(EC_KEY_FACTORY_ALGORITHM)` —
constants, often in another file. Adding intra- and **cross-file** constant propagation
(resolved from the source tree itself, no compiled classpath needed) recovered **16**
additional true-positive call sites: californium jumped from 9 to 23 (it centralises
algorithm names in constants like `EC_KEY_FACTORY_ALGORITHM = "EC"`), mina-sshd from 5 to
7 (`KeyUtils.RSA_ALGORITHM`, `KeyUtils.EC_ALGORITHM`). Spot-checked: all genuine.

**3. What constant propagation still cannot see marks the real detection frontier.**
Two patterns remain invisible and are honest limitations, not bugs:
- **Enum / registry selection** — jjwt chooses algorithms through its `SignatureAlgorithm`
  enum, which maps names internally. This is library semantics, not constant propagation;
  jjwt stayed at 0 call sites. Resolving it would need per-library modelling or full type
  resolution.
- **Runtime-dynamic selection** — `getInstance(algo)` from a method parameter, or
  `getInstance(builtIn.getTransformation())` from a method call (common in mina-sshd's
  negotiation layer). These are genuinely not statically determinable without dataflow.

## Per-codebase readiness profile

### jjwt 0.13.0 — HIGH type-coupling, algorithm selection invisible (by enum)
193 findings, **all** F4. Zero call sites even after constant propagation, because jjwt
resolves algorithms through enums rather than constants. Honest read: the migration
effort here is **API-surface churn** (key interfaces → PQC-capable abstractions), which
the tool captures well; the enum-internal algorithm wiring it does not see at all.

### mina-sshd 2.13.1 — largest surface, protocol-shaped
323 findings (316 F4 + 7 call sites, 2 recovered via cross-file constants). Broadest
key-type coupling of the four, including DSA (SSH still carries legacy host-key types) —
a genuine extra migration axis. Most of its algorithm selection flows through negotiation
constants and factory methods that remain dynamic. Pinned pre-PQC on purpose: upstream
later added ML-KEM hybrids (3.0.0-M2), so this is the natural Phase-2 candidate if a real
migration is ever mined.

### californium 3.14.0 — now the richest call-site signal
Constant propagation transformed this codebase's report: 20 → 34 findings, call sites
9 → 23. Californium centralises algorithm names in cross-file constants
(`EC_KEY_FACTORY_ALGORITHM`, `EDDSA`, `ED25519`, `ED448`, plus `TLSv1.2` version pins in
test/utility harnesses), all now resolved. It is the clearest example of why cross-file
resolution matters on real code, and the codebase whose score most reflects *active*
crypto usage rather than type coupling alone.

### shiro 2.2.1 — correctly near-zero (a useful negative control)
**Zero findings, and that appears correct.** 0 `Signature.getInstance`, 0
`KeyPairGenerator`, 0 concrete asymmetric key types; Shiro's cryptography is predominantly
**symmetric** (AES/Blowfish) and password hashing — not quantum-vulnerable in the
asymmetric sense and out of scope by design (doc 01). Its `Cipher.getInstance` calls take
a *variable* transformation that is both symmetric and dynamic. Shiro validates that the
detector does **not** false-positive on symmetric crypto.

## What this means

**For the tool.** Constant propagation was the right next step and paid off on real code
(call-site detection 14 → 30, +114%), while confirming its own frontier: enum/registry
selection and runtime-dynamic selection are what remain. Those, not literal vs. constant,
are where further detection investment would go. The F4 type-coupling signal remains the
workhorse and should be trusted as an API-surface indicator.

**For a migration planner.** Budget these libraries' migrations around **key-typed API
surface** first (interfaces, casts, key-material serialization), with algorithm-site
counts as a *lower bound* — especially for enum-driven libraries like jjwt, where the
visible call-site count is zero but the real work is not.

**For the scoring model.** A future `v1` might report type-coupling separately from active
call sites so a "coupling-only CRITICAL" is distinguishable from an "active-usage
CRITICAL." Noted for Phase 2; `v0` stays frozen.

## Caveats

- Detection is syntactic; call-site counts are still lower bounds (enum/dynamic selection
  is invisible — see headline 3).
- No ground truth here: precision/recall and score-vs-effort validation are Phase 2.
- Scores/tiers are heuristics; there is deliberately no engineer-days figure.
- Reports reflect the exact pinned commits in `pre-scan.md`; later releases differ (e.g.
  mina-sshd ≥ 2.13.2 and 3.0.0-M2 already add PQC).
