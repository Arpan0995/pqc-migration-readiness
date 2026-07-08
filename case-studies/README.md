# Case Studies

Target codebases and manual migration logs used to validate the auditor's
readiness scoring against real measured migration effort (see the project's
research question).

This directory is currently a stub. Case-study codebases will be added later
as git submodules (`git submodule add <repo-url> case-studies/<name>`) for
reproducibility, so avoid placing non-submodule content directly under a
case-study's own directory name.

Planned structure once populated:

```
case-studies/
├── <codebase-name>/       (git submodule, pinned to a specific commit)
├── <codebase-name>-migration-log.md   (manual migration effort log)
└── ...
```
