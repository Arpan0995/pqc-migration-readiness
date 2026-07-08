# 06 — Agility Provider Design Notes

Design input for the `agility-provider` module (step 3). Framing follows NIST CSWP 39 (*Considerations for Achieving Crypto Agility*, final Dec 2025): agility = the capability to replace algorithms **without breaking operations**, which means policy, negotiation, and audit — not just a wrapper API.

## 1. Shape: facade + policy engine (not a JCA `Provider`, in v1)

A custom `java.security.Provider` would be the "native" JCA integration, but it drags in provider-ranking, `getInstance` string design, and JAR-signing concerns that add nothing to the research questions. v1 is a **facade**:

```java
CryptoContext ctx = CryptoContext.fromPolicy(Path.of("crypto-policy.yaml"));
KemSession kem = ctx.keyEstablishment().negotiate(peerCapabilities);   // returns selected suite + API
SignerVerifier sig = ctx.signatures().forPolicy();                      // dual-sign per policy
```

Underneath, everything delegates to BC (JDK 21) through `getInstance` — the provider is a **policy field**, so JDK-native algorithms (25 LTS+) become a config change later, which is itself a nice demo of the agility argument.

## 2. Modes and suites

- Modes: `CLASSICAL`, `HYBRID`, `PQC_ONLY` — selected per *intent* (key-establishment vs signature), not globally; real migrations move the two at different speeds (HNDL makes KEM urgent first — doc 01).
- Suites are named, versioned tuples, e.g. `KE-HYBRID-X25519-MLKEM768`, `SIG-DUAL-ECDSAP256-MLDSA65`, `SIG-MLDSA65`. Names appear in policy, negotiation offers, and audit logs — one vocabulary everywhere.

## 3. Hybrid composition (delegating primitives to BC; only combination logic is ours)

- **KEM combiner**: run both key agreements, derive `ss = KDF(ss_classical || ss_pqc || transcript)` — the pattern standardized by X25519MLKEM768 (draft-ietf-tls-ecdhe-mlkem). HKDF-SHA-256 as KDF. Security holds if *either* component holds.
- **Dual signature**: sign with both; verification requires **both** valid (AND semantics — fail-closed). Encoding: length-prefixed concatenation with suite ID header (deliberately simple; composite-signature standardization is still fluid, and we should not invent a "clever" format).

## 4. Capability negotiation

Model mirrors TLS named-groups / SSH kex lists — familiar and analyzable:

1. Each party publishes a **capability descriptor**: ordered suite IDs per intent + minimum acceptable mode.
2. Selection = highest-preference suite in the *initiator's* order present in both lists (deterministic, no round-trips in-process; the descriptor exchange is the caller's transport problem).
3. No intersection → policy decides: `fail-closed` (default) or `downgrade-to` with an explicit floor. Downgrade events always audit-log at WARN.

Negotiation scenarios (classical-only peer, hybrid peer, PQC-only peer, mismatch) are exactly the benchmark scenarios in doc 05 — one implementation serves both.

## 5. Policy file (YAML, versioned schema)

```yaml
policyVersion: 1
provider: BC                    # BC | SunJCE-native (future)
keyEstablishment:
  mode: HYBRID
  suites: [KE-HYBRID-X25519-MLKEM768, KE-CLASSICAL-X25519]
  onNoIntersection: fail-closed
signatures:
  mode: HYBRID
  suites: [SIG-DUAL-ECDSAP256-MLDSA65, SIG-CLASSICAL-ECDSAP256]
  onNoIntersection: fail-closed
audit:
  sink: jsonl
  path: crypto-audit.jsonl
```

Hot-reload is out of scope for v1 (state a TODO); config-at-startup is enough for the research claims.

## 6. Audit log

JSONL, one event per operation: `{ts, op, intent, mode, suite, peerOffer, policyId, durationNanos, outcome}`. Three consumers: (a) the CSWP-39-style audit requirement, (b) benchmark instrumentation cross-check, (c) migration evidence ("prove we stopped using classical-only on date X").

## 7. Testing strategy

- Differential tests of every primitive path against **BC's bundled NIST KAT vectors** (ML-KEM encaps/decaps, ML-DSA sign/verify) — never hand-rolled vectors (plan §7).
- Property tests for the combiner: hybrid shared secret changes if either component's input changes; dual-sig verification fails if either signature is corrupted.
- Negotiation: exhaustive table tests over mode × capability × policy matrix (small, enumerable).
- Thread-safety: `getInstance` objects are not thread-safe — the facade owns instance lifecycle (per-call or pooled; decide at implementation, benchmark both if cheap).

## 8. Explicit non-goals (v1)

No custom primitive math (plan §7 scope boundary); no TLS stack integration (we model negotiation in-process; JEP 527 covers real TLS in JDK 27); no key-management/rotation subsystem (audit log records usage, not custody); no hot-reload.
