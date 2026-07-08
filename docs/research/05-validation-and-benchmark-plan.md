# 05 — Validation & Benchmark Plan

This is the step-4 playbook: what gets measured, how it is analyzed, and what each outcome would mean.

## 1. Detector accuracy (plan §4a)

- **Ground truth**: hand-labeled vulnerable-usage sites in each selected case study. Labeling happens from raw source (imports/call sites), *before* reading auditor output for that codebase; labels join to findings on rule IDs + file:line.
- **TP** = flagged site that genuinely requires change for a PQC migration. **FP** = flagged site needing no change (e.g., dead code path, test fixture — judgment logged). **FN** = labeled site the auditor missed (found by manual sweep of `java.security`/`javax.crypto`/BC imports).
- **Report**: precision / recall / F1 overall, per rule family (JCA entry point, TLS, JOSE, concrete-type), and per confidence level. Confidence calibration matters: HIGH findings should have near-1.0 precision; LOW findings are allowed to be noisy (they exist to drive F5 credit, not alarms).
- Single-rater mitigation: a second labeling pass after a ≥2-week washout on a 20% sample; report self-agreement (a poor-man's κ). Honest limitation, stated as such.

## 2. Score validity — the core test (plan §4b)

- **Data**: per-module predicted score S (frozen v0, doc 03) vs. measured effort vector (doc 04): files touched, LOC changed, breakages, test failures, minutes (Track A only).
- **Statistics**:
  - Primary: **Spearman ρ** between S and each effort metric (rank-based — right for small n, non-normal effort data). Secondary: Kendall τ.
  - **Comparative claim (H1)**: paired bootstrap over modules (≥10k resamples) for Δρ = ρ(S, effort) − ρ(B0, effort), with 95% CI. H1 supported iff CI excludes 0 in favor of S.
  - **Size control**: partial Spearman of S vs effort controlling module LOC; plus ρ(LOC, effort) reported as the trivial second baseline. S must survive both.
  - Target n: ≥5 codebases, ≥30 modules pooled. Report per-codebase and pooled (pooled with codebase as a stratum in the bootstrap).
- **Key figure**: scatter of predicted S vs. measured effort (one point per module, shape = codebase, color = track), with ρ, τ, and the S-vs-B0 comparison inset.
- **Interpretation grid** (decided before seeing data):

| Outcome | Inference |
|---|---|
| ρ(S) high, Δρ > 0 | H1 supported: fragility signals predict cost beyond counting — static triage is viable; the report's hotspot ranking is trustworthy |
| ρ(S) high, Δρ ≈ 0 | Counting is enough; fragility indicators add no signal (linters suffice — still useful, deflationary) |
| ρ(S) low everywhere | Static signals insufficient; migration cost is dominated by factors invisible to static analysis (negative result, publishable, redirects the field to dynamic/organizational measures) |
| ρ high on Track A, low on Track B (or vice versa) | Effort metrics or simulated-migration realism are suspect — investigate before claiming anything |

## 3. Threats to validity (write-up must include)

Single rater (scorer = migrator on Track A — mitigated by pre-registration + Track B); simulated migrations may miss organizational cost; case-study selection bias (mitigated: selection uses wave-1 counts only); BC-specific API shapes may not generalize to other providers; small n (mitigated: module-level pooling, rank statistics, CIs not p-values).

## 4. Agility-layer runtime benchmarks (plan §4c)

**Matrix** (JMH, `benchmarks` module):

| Axis | Values |
|---|---|
| Operation | KEM keygen / encaps / decaps; sign / verify |
| Algorithm | RSA-2048, RSA-3072, ECDSA-P256, Ed25519, X25519(ECDH), ML-KEM-768, ML-DSA-65, SLH-DSA-SHA2-128s, hybrid X25519+ML-KEM-768, dual-sig ECDSA+ML-DSA-65 |
| Path | direct Bouncy Castle; via agility-provider (policy resolved); via agility-provider incl. capability negotiation |

**Headline number**: relative overhead (agility − direct)/direct per operation — the standard objection ("your abstraction is too slow") answered with data. Negotiation cost reported separately (expected ns–µs; it's a table intersection).

**Scenarios**: peer classical-only / hybrid-capable / PQC-only / mismatch → fallback vs fail-closed policy paths (each exercises a different negotiation branch).

**JMH configuration**: `@Fork(3)`, `@Warmup(iterations = 5)`, `@Measurement(iterations = 5)`, Throughput + SampleTime (p50/p99) modes, `-prof gc` for allocation/op, Blackhole discipline, parameterized `@State` for algorithm/mode. SLH-DSA gets reduced iteration counts (slow by design; note it).

**JVM-specific observables** (the unstudied-territory claim from doc 01): allocation pressure and GC behavior from multi-KB signatures/keyshares vs. 64-byte classical equivalents; steady-state vs. cold JIT differences across modes. Fixed JDK 21 + pinned BC version; hardware documented in the report.

**Interpretation**: overhead <5% → agility layer adoption case holds; sizes dominating latency/allocations → quantified JVM-specific finding either way.

## 5. Deliverables checklist (step 4 outputs)

1. Per-codebase readiness report (JSON + markdown, doc 03 schema).
2. The correlation figure (scatter + ρ/τ + Δρ inset) — the paper's key result.
3. Precision/recall/F1 table per rule family and confidence level.
4. Benchmark tables/plots: overhead %, latency distributions, alloc/op, payload sizes.
5. Threats-to-validity section drafted from §3 above.
