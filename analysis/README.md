# Analysis harness (step 4)

Tests the core research claim: does the auditor's difficulty score **S** predict
measured migration effort better than the naive baseline **B0** (count of vulnerable
call sites), controlling for module size (LOC)? See
[`../docs/research/05-validation-and-benchmark-plan.md`](../docs/research/05-validation-and-benchmark-plan.md).

`correlate.py` is pure Python 3.9+ standard library — no numpy/scipy, so it runs
anywhere and results are reproducible. It computes Spearman ρ, Kendall τ, a partial
correlation controlling for LOC, and a bootstrap 95% CI for Δρ = ρ(S) − ρ(B0).

## Inputs (real data only)

1. **Auditor reports** — one `readiness-report.json` per case-study codebase,
   produced by the auditor (`--report` is repeatable to pool codebases).
2. **Effort log** — `effort-log.csv` in the format of
   [`effort-log.template.csv`](effort-log.template.csv), recording measured effort per
   module. `codebase` + `module` must match the report JSON.

The harness **joins on (codebase, module)** and never invents effort numbers.

## Data dependency (why there are no results yet)

The effort data is a **human-in-the-loop deliverable**, not something the tool can
produce:

- **Track A** — manually migrate selected codebases to hybrid crypto and log effort
  (files touched, lines changed, breakages, time). Protocol in
  [`../docs/research/04-case-study-plan.md`](../docs/research/04-case-study-plan.md) §5.
- **Track B** — mine projects that already migrated (e.g. Apache Mina SSHD's ML-KEM
  hybrid work) and measure the historical diffs.

Until at least ~3 modules of real data exist, `analyse()` refuses to report a
correlation. This is deliberate: the whole point of the project is that the numbers
be real and the model be frozen (`v0`) before they are collected.

## Verify the harness math (synthetic self-test)

```
python3 correlate.py --selftest
```

Generates clearly-labelled **synthetic** modules where effort depends on a hidden
fragility structure that S captures and B0 does not, then asserts the harness
recovers ρ(S) > ρ(B0). This validates the statistics only — it is never a
case-study result.

## Run on real data (once collected)

```
python3 correlate.py \
    --report ../case-studies/jjwt/audit-report.json \
    --report ../case-studies/mina-sshd/audit-report.json \
    --effort effort-log.csv \
    --metric lines_changed
```

Repeat with `--metric files_touched|build_breakages|test_failures|minutes` to check
robustness across effort measures, as the validation plan requires.
