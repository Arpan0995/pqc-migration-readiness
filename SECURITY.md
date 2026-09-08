# Security policy

## Reporting a problem

Please do not open a public issue for anything security-sensitive. Use one of
these channels instead:

- GitHub's private vulnerability reporting: the "Report a vulnerability" button
  on the [Security tab](https://github.com/Arpan0995/pqc-migration-readiness/security)
  opens a private advisory that only you and the maintainer can see.
- Email: arpansharma073@gmail.com, the address in `CITATION.cff`.

Include the version or commit, what you found, and the steps or the input that
reproduce it. You will get an acknowledgement, then a fix or a reasoned decision,
and credit in the release notes if you want it.

## What counts as a security issue

- **Auditor, Maven plugin, GitHub Action.** The scanner parses untrusted Java
  source with JavaParser. Anything that lets a crafted source tree execute code,
  read or write files outside the output directory, or steer the workflow beyond
  the report it writes.
- **Agility provider.** Anything that makes a negotiated mode weaker than the
  policy says: a downgrade the negotiation accepts, a hybrid combiner that does
  not bind both of its inputs, key material reaching the audit log.

## What does not

False negatives and false positives in detection, scoring disagreements and
documentation mistakes are ordinary issues; please use the issue templates.
Dependency alerts with no reachable path are welcome as ordinary issues too.

## Supported versions

Fixes go into the next release on Maven Central. Older releases are not patched.
The agility provider is research code and is not intended for production use;
see the README.
