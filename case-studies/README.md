# Case Studies

Real public codebases the auditor runs against. The project currently runs in
two phases (see [`docs/research/03-difficulty-scoring-model.md` §8](../docs/research/03-difficulty-scoring-model.md)):

- **Phase 1 (current)**: pin a codebase, run the auditor, publish its
  readiness report (score, effort tier, ranked hotspots) as an **estimate**.
  No migration is performed.
- **Phase 2 (deferred, not currently active)**: migrate the pinned codebase
  (or mine one that already migrated for real) and log actual effort, to
  validate the score against measured effort — see
  [`docs/research/04-case-study-plan.md`](../docs/research/04-case-study-plan.md).

This directory is currently a stub. Case-study codebases are added as git
submodules (`git submodule add <repo-url> case-studies/<name>`), pinned to a
specific commit, so the exact code a report describes is always reproducible
— even though Phase 1 doesn't modify that code at all.

Planned structure:

```
case-studies/
├── pre-scan.md                        (candidate shortlist + selection rationale)
└── <codebase-name>/
    ├── target/                        (git submodule, pinned to a specific commit)
    ├── readiness-report.json          (Phase 1: auditor output)
    ├── readiness-report.md            (Phase 1: auditor output)
    └── effort-log.md                  (Phase 2 only, once that phase starts)
```
