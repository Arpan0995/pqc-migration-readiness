# 03 — Difficulty Scoring Model (v0, pre-registered)

**Pre-registration statement.** This model (weights included) is frozen **before** any migration-effort ground truth is collected, and the freeze is timestamped by git history. If validation results later motivate changes, the tuned model becomes **v1** and may only be evaluated on codebases *not used for tuning*. This is the bias control demanded by plan §4b.3 — without it, "our score correlates with effort" is circular.

## 1. Units of analysis

- **Finding** — one flagged site (rule ID + file:line, doc 02).
- **File score** — sum of finding difficulties in the file.
- **Module score** — the unit validated against effort: Maven/Gradle module, or top-level package for single-module projects.

## 2. Per-finding difficulty

```
d(finding) = base(category) × Π multipliers   (product capped at 6.0)
```

**Base weights**

| Category | base |
|---|---|
| Key establishment / asymmetric encryption call site (Cipher-RSA, KeyAgreement, KEM) | 3 |
| Signature call site (sign or verify) | 3 |
| Key generation / key factory / spec construction | 2 |
| TLS configuration surface | 2 |
| JOSE/JWT algorithm pin | 2 |
| Concrete key-type coupling site (F4, counted per distinct site) | 1 |
| Informational (AES-128, SHA-1, …) | 0 (reported, never scored) |

**Fragility multipliers** (from doc 02; a finding takes every multiplier whose indicator co-occurs with it)

| Indicator | × |
|---|---|
| F1 fixed-size buffer adjacent | 1.5 |
| F2 fixed-width persistence / wire format | 2.0 |
| F3 protocol/suite pinning | 1.5 |
| F4 concrete-type coupling on the same value | 1.5 |
| F6 persisted key material | 2.0 |
| F8 third-party API boundary | 2.5 |
| F5 algorithm name config-sourced (agility credit) | 0.5 |

## 3. Module score

```
S(module) = ( Σ d(finding) ) × spread,   spread = 1 + 0.1 × log2(1 + files_with_findings)
```

Rationale: 40 findings in one file are one focused rewrite; 40 findings across 25 files are a campaign. The log keeps the term gentle; the constant 0.1 is a v0 guess — deliberately pre-registered rather than tuned.

## 4. Urgency (separate axis, never mixed into S)

```
U(module) = Σ base(category) × w_urgency,   w = 2.0 for confidentiality (HNDL), 1.0 for signatures
```

The report ranks by S (cost) and colors by U (deadline pressure). Validation (doc 05) tests **S only** — U is a planning aid whose "validation" is the IR 8547 threat model, not our data.

## 5. Baseline comparator

```
B0(module) = raw count of vulnerable call sites (waves 1–2 findings, unweighted)
```

H1 (doc 01) is a *comparative* claim: corr(S, effort) > corr(B0, effort). B0 is what any grep/linter/CBOM inventory effectively gives you. If S cannot beat B0, the fragility hypothesis is falsified regardless of how good corr(S, effort) looks in isolation.

## 6. Confounder to control: module size

LOC correlates with everything (more code → more findings → more effort). Doc 05 therefore also reports partial correlations controlling for module LOC, and corr(LOC, effort) alone as a second trivial baseline. S must add signal beyond "big modules are hard."

## 7. Output schema (report JSON, consumed by step-4 analysis)

```json
{
  "codebase": "...", "auditorVersion": "...", "scoreModel": "v0",
  "modules": [{
    "name": "...", "loc": 0, "score": 0.0, "tier": "MEDIUM", "urgency": 0.0, "baselineCount": 0,
    "files": [{
      "path": "...",
      "findings": [{
        "ruleId": "JCA-KPG-RSA", "line": 0, "api": "KeyPairGenerator",
        "algorithm": "RSA", "category": "KEYGEN", "confidence": "HIGH",
        "fragility": ["F1", "F4"], "difficulty": 4.5, "urgencyClass": "SIGNATURE",
        "snippet": "..."
      }]
    }]
  }]
}
```

Human-readable report (markdown) renders from this JSON: ranked hotspot list with file:line, matched rule, *why it is expensive* (the fragility indicators in plain language), and suggested migration action (e.g., "replace fixed 256-byte signature buffer with length-prefixed framing").

## 8. Project phasing: estimation now, validation later

The original plan (this document, doc 04, doc 05) was written as a single validated study: freeze the score, migrate real codebases, measure actual effort, correlate. That full plan is **deferred**, not abandoned, because real migrations (whether we perform them or mine a project's history) are a significant, separate effort. The project now runs in two phases:

- **Phase 1 (current)** — the *estimation* phase. The auditor runs against real public codebases and produces the score `S`, the naive baseline `B0`, and a derived qualitative **effort tier** (`EffortTier`: `NONE < LOW < MEDIUM < HIGH < CRITICAL`, thresholds at score 0 / 0.01 / 10 / 40 / 120 — see `auditor/.../model/EffortTier.java`). This is a transparent, structured heuristic informed by the fragility indicators in doc 02, comparable in spirit to how CBOM-style tools already rank findings. **It is not a validated prediction.** There is deliberately no "N engineer-days" figure anywhere in the tool, because no conversion factor from score to hours has been tested against anything — publishing one would imply a precision we don't have.
- **Phase 2 (deferred)** — the *validation* phase, unchanged from doc 04/05: real or mined migrations, measured effort, correlation of `S` against effort, `S` vs. baseline `B0` comparison. Doc 04 and doc 05 stay as-written as the protocol for when this phase starts; nothing in them needs to change.

Consequence for how to read any report this tool produces in Phase 1: the module ranking and hotspot list are useful for **triage and prioritization** (which modules and sites look expensive, and why, per the fragility indicators) — but a claim like "score correlates with real effort" or "tier X means Y engineer-days" is not yet substantiated. Report language reflects this (see the caveat banner in the Markdown report and this section).
