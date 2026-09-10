---
title: PQC Migration Readiness
---

# PQC Migration Readiness

A static auditor that estimates how much a post-quantum cryptography migration would take for a Java codebase. It finds quantum-vulnerable JCA usage and the structural patterns that make a migration expensive, ranks the hotspots, and produces an ordered migration plan with an engineer-time estimate.

## Start here

- **[PQC readiness of the Java ecosystem](ecosystem-scan.html)**: the auditor run across 27 widely used Java projects, ranked by estimated migration effort.
- **[Source, releases and issues](https://github.com/Arpan0995/pqc-migration-readiness)**: the repository on GitHub.
- **[Project overview](https://github.com/Arpan0995/pqc-migration-readiness/blob/main/docs/PROJECT.md)**: what is built, key results, and current state.

## Research documentation

Design decisions and methodology, rendered on GitHub:

- [Background and motivation](https://github.com/Arpan0995/pqc-migration-readiness/blob/main/docs/research/01-background-and-motivation.md)
- [Detection rule catalog](https://github.com/Arpan0995/pqc-migration-readiness/blob/main/docs/research/02-detection-rule-catalog.md)
- [Difficulty scoring model](https://github.com/Arpan0995/pqc-migration-readiness/blob/main/docs/research/03-difficulty-scoring-model.md)
- [Case-study plan](https://github.com/Arpan0995/pqc-migration-readiness/blob/main/docs/research/04-case-study-plan.md)
- [Validation and benchmark plan](https://github.com/Arpan0995/pqc-migration-readiness/blob/main/docs/research/05-validation-and-benchmark-plan.md)
- [Agility provider design](https://github.com/Arpan0995/pqc-migration-readiness/blob/main/docs/research/06-agility-provider-design.md)

Apache-2.0 licensed.
