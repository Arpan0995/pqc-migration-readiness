## What this changes

## Why

## Related issue

Closes #

<!-- Use a closing keyword (Closes, Fixes or Resolves #NN) so the linked issue closes automatically when this PR merges. Delete this section if the PR is not tied to an issue. -->

## Checklist

- [ ] `mvn verify -Dgpg.skip=true` passes on JDK 21
- [ ] Tests added or updated. A new detection rule needs one fixture where it fires and one where it stays quiet.
- [ ] `docs/research/02-detection-rule-catalog.md` or `03-difficulty-scoring-model.md` updated if a rule or a weight changed
