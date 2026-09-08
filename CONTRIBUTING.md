# Contributing

Thanks for looking at the PQC Migration Readiness Framework. The project is in
its estimation phase (Phase 1 in the README), and outside input will turn the
current heuristic score into a validated one. Questions, scan results from real
codebases, bug reports and pull requests are all useful, and each one gets read
and answered.

## Ways to take part

### Start a Discussion

[GitHub Discussions](https://github.com/Arpan0995/pqc-migration-readiness/discussions)
is the place for anything that is not yet a concrete bug or task:

- questions about the approach, the scoring model, or how to read a report;
- "I ran the auditor on X" reports, with the hotspots you agree or disagree with;
- ideas for new detection rules or for the agility layer;
- pointers to related tools, papers, or standards activity.

Mention the auditor version (1.4.0, for example) and the JDK you used, and paste
the relevant part of `readiness-report.md` if it helps make the point.

### Raise an Issue

Open an [Issue](https://github.com/Arpan0995/pqc-migration-readiness/issues)
for:

- crashes or wrong output from the auditor, the Maven plugin, or the agility
  provider;
- false positives and false negatives in detection: a `getInstance` call that
  was missed, or a flagged call that is not quantum-vulnerable;
- scores or effort tiers that look wrong for a reason you can explain;
- mistakes in the documentation;
- feature requests.

A good bug report has the version or commit, the JDK and OS, the exact command
you ran, a minimal Java snippet that reproduces the problem, and what you
expected instead. For detection gaps the snippet is the most valuable part.

### Open a Pull Request

Pull requests are welcome, from typo fixes to new detection rules. For anything
larger than a small fix, please open an Issue or a Discussion first so the
direction is settled before you spend time on it.

The usual flow:

1. Fork the repository and create a branch.
2. Build and run the tests with JDK 21 and Maven 3.9+:

   ```
   mvn clean install -Dgpg.skip=true
   ```

   The flag skips the GPG signing step that release builds run in `verify`;
   without it the build fails unless you have a signing key configured.

   To build the CLI jar on its own:

   ```
   mvn -pl auditor -am package
   ```

3. Add or update tests for the change. A new detection rule needs one fixture
   that shows it firing and one that shows it staying quiet.
4. Keep the change focused on one thing, and say in the PR description what it
   does and why.

Two project-specific points:

- The difficulty score (score v0) is pre-registered: its weights were frozen
  before any effort data was collected, and that is the point of the study.
  Changes to the weights, or to what the detector counts, need a Discussion
  first and a matching update to `docs/research/02-detection-rule-catalog.md`
  or `docs/research/03-difficulty-scoring-model.md`, so the record of what
  changed and when stays intact.
- The agility provider is research code, not a production crypto library.
  Changes there should keep to the policy, negotiation, and audit-log design in
  `docs/research/06-agility-provider-design.md`.

### Star the repository

If you find this project interesting or useful, please star it. A star is the
easiest signal that this line of work is worth continuing, and it helps other
people working on Java PQC migration find the repository.

## Where help is most useful right now

- **Scan results from real codebases.** Run the auditor on a Java project you
  know well and share the report in a Discussion, with your view of whether the
  ranking matches reality. This is the raw material Phase 2 needs.
- **Migration effort data.** If you have migrated, or are migrating, a Java
  codebase to hybrid or PQC algorithms, `docs/research/04-case-study-plan.md`
  describes what to record.
- **Remaining detection rules.** F2 (fixed-width persistence), F8 (third-party
  API boundary), the JOSE/JWT surface, and enum/registry or dataflow modelling
  for dynamic algorithm selection are all open.
- **Benchmark reproductions.** The JMH results in
  `benchmarks/results/RESULTS.md` come from one machine. Numbers from other
  hardware and JDK builds would be valuable.
- **Documentation.** Anything unclear in the README or the research docs is
  worth an Issue or a direct fix.

## Ground rules

- Be specific and be courteous. Disagreement about a score or a rule is fine;
  keep it about the code and the data.
- Security-sensitive findings do not belong in public Issues. Email the address
  in `CITATION.cff` instead.
- By contributing you agree that your contribution is licensed under the
  project's [Apache License 2.0](LICENSE). There is no CLA.
