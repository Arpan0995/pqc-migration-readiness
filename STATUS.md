# Release engineering status — v1.0.0 Maven Central deployment

Date: 2026-07-14. All results below are from real runs on this machine
(macOS, Homebrew OpenJDK 21.0.11, Maven 3.9.16).

## Deployment (Step 5) — STOPPED AT VALIDATED, awaiting manual publish

- **Deployment ID: `1e42fc41-7d1d-479e-9303-086758ab855f`**
- State: **VALIDATED** (autoPublish=false). Nothing has been published to the
  live repository. Review and publish at
  https://central.sonatype.com/publishing/deployments
- Bundle contents (all GPG-signed with `.asc`):
  - `io.github.arpan0995:pqc-migration-readiness:1.0.0` — parent POM only (pom packaging)
  - `io.github.arpan0995:pqc-readiness-auditor:1.0.0` — thin jar, `-all` fat jar, `-sources`, `-javadoc`, POM
  - `io.github.arpan0995:pqc-readiness-agility:1.0.0` — thin jar, `-sources`, `-javadoc`, POM
  - benchmarks module: **not in the bundle** (excluded via `-pl .,auditor,agility-provider`,
    plus `skipPublishing=true` and `maven.deploy.skip=true` as belt-and-braces)
- Central validation warnings: **none** reported by the portal; the deployment
  went straight to VALIDATED.

## What was changed

- Coordinates: `org.pqcreadiness` → `io.github.arpan0995` (verified namespace);
  artifactIds `auditor` → `pqc-readiness-auditor`, `agility-provider` → `pqc-readiness-agility`.
  Java package names unchanged (`org.pqcreadiness.*`) — Central does not require
  package = groupId, and renaming would have touched frozen code.
- Full Central-required metadata (name, description, url, licenses, developers,
  scm with `<tag>v1.0.0</tag>`) added explicitly to parent + both published POMs.
- `LICENSE` (canonical Apache-2.0 text, sha1 `2b8b8152…` matches apache.org copy)
  and `CITATION.cff` added at repo root. **No license headers added to sources** —
  the project had no existing header convention (all files start with `package`).
- Unused Bouncy Castle dependency **removed from the auditor POM** (zero
  `org.bouncycastle` references in auditor sources; see dependency:tree below).
- benchmarks POM: parent/dependency coordinates updated; publishing, javadoc,
  sources, and gpg skipped for that module.
- No changes to scoring-model weights, benchmark code, or pinned submodules.
- Tag `v1.0.0` moved (with approval) from `4e879bf` to release commit `1621227`
  so the tagged tree contains exactly what was deployed.

## Plugin versions used

| Plugin | Version | Notes |
|---|---|---|
| central-publishing-maven-plugin | **0.11.0** | Task said "latest 0.8.x", but 0.11.0 is the current latest (0.8.x is no longer newest); `publishingServerId=central`, `autoPublish=false` |
| maven-source-plugin | 3.3.1 | latest stable (4.0.0 is beta); `jar-no-fork` |
| maven-javadoc-plugin | 3.12.0 | `jar` goal |
| maven-gpg-plugin | 3.2.8 | `sign` at `verify` phase |
| maven-shade-plugin | 3.6.2 | already managed by the project |

## Javadoc route taken

**No workaround was needed.** `mvn clean verify` passed with javadoc generation
enabled and default doclint. The build emits ~30 non-fatal javadoc *warnings*
(missing `@param`/`@return`/comments, mostly in `agility-provider`), but no
errors. `<doclint>none</doclint>` was NOT set; javadoc jars are real.

## Test results

`mvn clean verify` (signed): **77/77 pass** — 57 auditor, 20 agility-provider,
0 failures/errors/skips. Matches expected counts.

## dependency:tree for the auditor (after removing unused BC dep)

```
io.github.arpan0995:pqc-readiness-auditor:jar:1.0.0
+- com.github.javaparser:javaparser-symbol-solver-core:jar:3.27.0:compile
|  +- com.github.javaparser:javaparser-core:jar:3.27.0:compile
|  +- org.javassist:javassist:jar:3.30.2-GA:compile
|  +- com.google.guava:guava:jar:33.4.8-jre:compile
|  |  +- com.google.guava:failureaccess:jar:1.0.3:compile
|  |  +- com.google.guava:listenablefuture:jar:9999.0-empty-to-avoid-conflict-with-guava:compile
|  |  +- org.jspecify:jspecify:jar:1.0.0:compile
|  |  +- com.google.errorprone:error_prone_annotations:jar:2.36.0:compile
|  |  \- com.google.j2objc:j2objc-annotations:jar:3.0.0:compile
|  \- org.checkerframework:checker-qual:jar:3.49.4:compile
+- com.fasterxml.jackson.core:jackson-databind:jar:2.18.2:compile
|  +- com.fasterxml.jackson.core:jackson-annotations:jar:2.18.2:compile
|  \- com.fasterxml.jackson.core:jackson-core:jar:2.18.2:compile
\- org.junit.jupiter:junit-jupiter:jar:6.1.1:test
   (junit subtree, test scope only)
```

**No Bouncy Castle anywhere in the tree** → shading is safe; the multi-release
jar flattening hazard documented in the paper does not apply to the auditor
fat jar. agility-provider (which does use BC) ships thin jar only, as required.

## Fat-jar verification (Step 4)

Built `pqc-readiness-auditor-1.0.0-all.jar` (8.4 MB, `Main-Class:
org.pqcreadiness.auditor.cli.AuditorCli`, attached as `all` classifier;
`createDependencyReducedPom=false` so the published POM keeps real deps).

Ran it against the pinned jjwt 0.13.0 case-study tree:

```
java -jar pqc-readiness-auditor-1.0.0-all.jar case-studies/jjwt/repo --name jjwt-0.13.0
Scanned 408 files (0 skipped), 193 findings across 3 module(s).
```

Field-by-field JSON comparison with the committed
`case-studies/jjwt/readiness-report.json`: **identical except** `generatedAt`
(timestamp) and `auditorVersion` (`0.1.0-SNAPSHOT` → `1.0.0`; the committed
report predates the version bump). All findings, scores, and module results match.

## Zenodo prep (Step 7)

- `CITATION.cff` added and pushed to all three repos (author Arpan Sharma; no
  ORCID supplied — say the word and I'll add it).
- Tags pushed, pointing at the paper-cited commits:
  - `pqc-migration-readiness` → `v1.0.0` at `1621227` (release commit)
  - `PQC-Java-Library-Comparison` → `v1.0.0` at `da87998` (cited commit)
  - `pqc-hybrid-vs-classical` → `v1.0.0` at `55be3dd` (cited commit)
- Caveat: for the two paper-pinned repos the CITATION.cff commit necessarily
  postdates the cited commit, so the Zenodo archive of `v1.0.0` will not
  contain CITATION.cff (metadata can be edited in Zenodo's UI after archiving).
- Neither PQC-Java-Library-Comparison nor pqc-hybrid-vs-classical has a
  LICENSE file; Zenodo will ask for a license when you publish the record.

## Environment fixes made along the way

- `~/.m2/settings.xml`: replaced the portal template placeholder
  `<id>${server}</id>` with `<id>central</id>` and wrapped the `<server>` entry
  in the required `<servers>` element (credentials were copied programmatically,
  never read or displayed); file permissions set to 0600.
- GPG signing done via gpg-agent passphrase caching (you primed the cache from
  your terminal); no key material was touched.

## Remaining (blocked on you)

1. Review deployment `1e42fc41-7d1d-479e-9303-086758ab855f` in the portal and
   click **Publish** (or Drop). Releases are immutable once published.
2. Toggle the three repos on in Zenodo's GitHub integration, then create a
   GitHub **release** from the existing `v1.0.0` tag in each repo (Zenodo
   archives on release-published).
3. Confirm publication → I then add README Install/Run section + Maven Central
   badges + repo topics, and DOI badges once the DOIs exist.
