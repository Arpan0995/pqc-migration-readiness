#!/usr/bin/env python3
"""Correlation analysis for the PQC migration readiness study (step 4).

Implements the analysis in docs/research/05-validation-and-benchmark-plan.md:
tests whether the auditor's difficulty score S predicts measured migration effort
better than the naive baseline B0 (count of vulnerable call sites), controlling for
module size (LOC).

This harness consumes REAL data:
  * one or more auditor report JSON files (module -> score, baselineCount, loc)
  * an effort CSV recording measured migration effort per module

It does NOT invent effort data. Until real case-study migrations are logged
(Track A) or mined (Track B) per docs/research/04-case-study-plan.md, run
`--selftest` to validate the statistics on clearly-labelled synthetic data.

Pure standard library (Python 3.9+): no numpy/scipy required, so results are
reproducible without any environment setup.
"""
from __future__ import annotations

import argparse
import csv
import json
import math
import random
import sys
from dataclasses import dataclass
from typing import Dict, List, Sequence, Tuple


# --------------------------------------------------------------------------- #
# Statistics (pure stdlib)                                                     #
# --------------------------------------------------------------------------- #

def rankdata(values: Sequence[float]) -> List[float]:
    """Average (fractional) ranks, so ties share the mean of their positions."""
    order = sorted(range(len(values)), key=lambda i: values[i])
    ranks = [0.0] * len(values)
    i = 0
    while i < len(order):
        j = i
        while j + 1 < len(order) and values[order[j + 1]] == values[order[i]]:
            j += 1
        avg = (i + j) / 2.0 + 1.0  # 1-based average rank
        for k in range(i, j + 1):
            ranks[order[k]] = avg
        i = j + 1
    return ranks


def pearson(x: Sequence[float], y: Sequence[float]) -> float:
    n = len(x)
    if n < 2:
        return float("nan")
    mx = sum(x) / n
    my = sum(y) / n
    sxy = sum((a - mx) * (b - my) for a, b in zip(x, y))
    sxx = sum((a - mx) ** 2 for a in x)
    syy = sum((b - my) ** 2 for b in y)
    if sxx == 0 or syy == 0:
        return float("nan")
    return sxy / math.sqrt(sxx * syy)


def spearman(x: Sequence[float], y: Sequence[float]) -> float:
    return pearson(rankdata(x), rankdata(y))


def kendall_tau(x: Sequence[float], y: Sequence[float]) -> float:
    n = len(x)
    if n < 2:
        return float("nan")
    concordant = discordant = 0
    for i in range(n):
        for j in range(i + 1, n):
            dx = x[i] - x[j]
            dy = y[i] - y[j]
            s = dx * dy
            if s > 0:
                concordant += 1
            elif s < 0:
                discordant += 1
    denom = concordant + discordant
    return (concordant - discordant) / denom if denom else float("nan")


def partial_spearman(x: Sequence[float], y: Sequence[float], z: Sequence[float]) -> float:
    """Rank-based partial correlation of x and y controlling for z."""
    rxy = spearman(x, y)
    rxz = spearman(x, z)
    ryz = spearman(y, z)
    denom = math.sqrt((1 - rxz ** 2) * (1 - ryz ** 2))
    if denom == 0 or math.isnan(denom):
        return float("nan")
    return (rxy - rxz * ryz) / denom


def bootstrap_delta_rho(
    score: Sequence[float],
    baseline: Sequence[float],
    effort: Sequence[float],
    iterations: int = 10000,
    seed: int = 12345,
) -> Tuple[float, float, float]:
    """Bootstrap the difference Delta-rho = rho(S, effort) - rho(B0, effort).

    Returns (point_estimate, ci_low_2.5%, ci_high_97.5%). H1 is supported when the
    CI excludes 0 in favour of S.
    """
    n = len(effort)
    point = spearman(score, effort) - spearman(baseline, effort)
    rng = random.Random(seed)
    deltas: List[float] = []
    for _ in range(iterations):
        idx = [rng.randrange(n) for _ in range(n)]
        s = [score[i] for i in idx]
        b = [baseline[i] for i in idx]
        e = [effort[i] for i in idx]
        d = spearman(s, e) - spearman(b, e)
        if not math.isnan(d):
            deltas.append(d)
    deltas.sort()
    lo = deltas[int(0.025 * len(deltas))]
    hi = deltas[int(0.975 * len(deltas)) - 1]
    return point, lo, hi


# --------------------------------------------------------------------------- #
# Data loading                                                                 #
# --------------------------------------------------------------------------- #

@dataclass
class ModuleRow:
    codebase: str
    module: str
    score: float
    baseline: int
    loc: int
    effort: float


def load_reports(paths: Sequence[str]) -> Dict[Tuple[str, str], dict]:
    """Map (codebase, module) -> {score, baseline, loc} from report JSON files."""
    out: Dict[Tuple[str, str], dict] = {}
    for path in paths:
        with open(path) as fh:
            report = json.load(fh)
        codebase = report.get("codebase", path)
        for m in report.get("modules", []):
            out[(codebase, m["name"])] = {
                "score": float(m["score"]),
                "baseline": int(m["baselineCount"]),
                "loc": int(m["loc"]),
            }
    return out


def load_effort(path: str, metric: str) -> Dict[Tuple[str, str], float]:
    """Load an effort CSV keyed by (codebase, module).

    Required columns: codebase, module, and the chosen effort metric column.
    """
    out: Dict[Tuple[str, str], float] = {}
    with open(path, newline="") as fh:
        reader = csv.DictReader(fh)
        if metric not in (reader.fieldnames or []):
            raise SystemExit(f"effort CSV has no column '{metric}'; columns: {reader.fieldnames}")
        for row in reader:
            out[(row["codebase"], row["module"])] = float(row[metric])
    return out


def join(reports: Dict[Tuple[str, str], dict],
         effort: Dict[Tuple[str, str], float]) -> List[ModuleRow]:
    rows: List[ModuleRow] = []
    for key, rep in reports.items():
        if key in effort:
            rows.append(ModuleRow(key[0], key[1], rep["score"], rep["baseline"],
                                  rep["loc"], effort[key]))
    return rows


# --------------------------------------------------------------------------- #
# Reporting                                                                    #
# --------------------------------------------------------------------------- #

def analyse(rows: List[ModuleRow], metric: str, banner: str = "") -> str:
    if len(rows) < 3:
        return (f"Only {len(rows)} joined module(s); need >= 3 for meaningful "
                f"correlation. Add more case-study data (docs/research/04).")
    score = [r.score for r in rows]
    baseline = [float(r.baseline) for r in rows]
    loc = [float(r.loc) for r in rows]
    effort = [r.effort for r in rows]

    rho_s = spearman(score, effort)
    rho_b = spearman(baseline, effort)
    rho_loc = spearman(loc, effort)
    tau_s = kendall_tau(score, effort)
    partial = partial_spearman(score, effort, loc)
    delta, lo, hi = bootstrap_delta_rho(score, baseline, effort)
    supported = lo > 0

    lines = []
    if banner:
        lines.append(banner)
    lines.append(f"# Score validity analysis (effort metric: {metric})\n")
    lines.append(f"- Modules (n): {len(rows)}")
    lines.append(f"- Spearman rho(S, effort):   {rho_s:+.3f}")
    lines.append(f"- Kendall  tau(S, effort):   {tau_s:+.3f}")
    lines.append(f"- Baseline rho(B0, effort):  {rho_b:+.3f}")
    lines.append(f"- Size     rho(LOC, effort): {rho_loc:+.3f}  (trivial baseline / confounder)")
    lines.append(f"- Partial  rho(S, effort | LOC): {partial:+.3f}  (S beyond module size)")
    lines.append(f"- Delta-rho = rho(S) - rho(B0): {delta:+.3f}  "
                 f"[95% CI {lo:+.3f}, {hi:+.3f}]")
    lines.append("")
    if supported:
        lines.append("=> H1 SUPPORTED at this data: S predicts effort better than counting "
                     "(Delta-rho CI excludes 0).")
    elif delta > 0:
        lines.append("=> H1 not yet supported: S trends better than B0 but the CI includes 0 "
                     "(need more data or effect is small).")
    else:
        lines.append("=> H1 not supported at this data: counting (B0) does as well as S. "
                     "See the interpretation grid in docs/research/05.")
    return "\n".join(lines)


# --------------------------------------------------------------------------- #
# Self-test on synthetic data (clearly labelled; never a case-study result)    #
# --------------------------------------------------------------------------- #

def selftest() -> str:
    """Generate synthetic modules where true effort depends on a fragility-aware
    'true difficulty' that S approximates better than B0, then confirm the harness
    recovers rho(S) > rho(B0). This validates the STATISTICS, not the hypothesis.
    """
    rng = random.Random(7)
    rows: List[ModuleRow] = []
    for i in range(24):
        call_sites = rng.randint(1, 40)                 # drives B0
        fragility = rng.uniform(0.5, 3.0)               # hidden difficulty structure
        loc = rng.randint(500, 20000)
        true_difficulty = call_sites * fragility
        # Effort is monotone in true difficulty plus noise; S encodes fragility, B0 does not.
        effort = true_difficulty * rng.uniform(0.8, 1.2) + rng.gauss(0, 3)
        score = call_sites * fragility * rng.uniform(0.9, 1.1)   # S ~ true difficulty
        rows.append(ModuleRow("SYNTHETIC", f"mod{i}", score, call_sites, loc, effort))

    banner = ("> SYNTHETIC SELF-TEST DATA -- validates the analysis math only.\n"
              "> These are NOT case-study results and must never be cited as findings.\n")
    out = analyse(rows, "synthetic_effort", banner)
    rho_s = spearman([r.score for r in rows], [r.effort for r in rows])
    rho_b = spearman([float(r.baseline) for r in rows], [r.effort for r in rows])
    ok = rho_s > rho_b
    out += f"\n\nSelf-test assertion rho(S) > rho(B0): {'PASS' if ok else 'FAIL'}"
    if not ok:
        raise SystemExit("Self-test FAILED: harness did not recover expected ordering.")
    return out


# --------------------------------------------------------------------------- #

def main(argv: Sequence[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--report", action="append", default=[],
                        help="path to an auditor readiness-report.json (repeatable)")
    parser.add_argument("--effort", help="path to the effort CSV")
    parser.add_argument("--metric", default="lines_changed",
                        help="effort column to correlate against (default: lines_changed)")
    parser.add_argument("--selftest", action="store_true",
                        help="run the synthetic self-test instead of real analysis")
    args = parser.parse_args(argv)

    if args.selftest:
        print(selftest())
        return 0

    if not args.report or not args.effort:
        parser.error("provide --report and --effort, or use --selftest")

    reports = load_reports(args.report)
    effort = load_effort(args.effort, args.metric)
    rows = join(reports, effort)
    print(analyse(rows, args.metric))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
