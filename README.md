# PQC Migration Readiness Framework

A research framework for assessing how costly it is to migrate a Java codebase
to post-quantum cryptography (PQC).

Two linked components:

- **`auditor`** — scans a Java codebase for quantum-vulnerable cryptographic
  usage and produces a migration difficulty score per module/file.
- **`agility-provider`** — a runtime abstraction layer over Java's JCA/JCE
  that lets an application switch between classical, hybrid, and PQC-only
  algorithms via policy/config rather than code changes.

Supporting modules:

- **`benchmarks`** — JMH harness measuring runtime cost of the agility layer
  across modes.
- **`case-studies`** — real open-source Java codebases used as auditor test
  subjects, plus manual migration effort logs (see
  [`case-studies/README.md`](case-studies/README.md)).

## Requirements

- JDK 21
- Maven 3.9+

## Build

```
mvn clean install
```

## Status

Early scaffolding stage. Detection and scoring logic are not yet implemented.

## License

TBD.
