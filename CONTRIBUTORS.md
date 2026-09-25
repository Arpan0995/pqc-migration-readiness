# Contributors

Thank you to everyone who has contributed to PQC Migration Readiness. Each line
records what a contributor added; follow the pull request for the detail.

- [@emilTejnRasmussen](https://github.com/emilTejnRasmussen): positive test fixture for `Cipher.getInstance("ECIES")` detection ([#32](https://github.com/Arpan0995/pqc-migration-readiness/pull/32))
- [@najuma223](https://github.com/najuma223): `--version` and `-v` flags for the auditor CLI ([#33](https://github.com/Arpan0995/pqc-migration-readiness/pull/33))
- [@zemiles](https://github.com/zemiles): ElGamal detection for the Cipher and KeyPairGenerator APIs ([#34](https://github.com/Arpan0995/pqc-migration-readiness/pull/34)) and Ed448/EdDSA signature detection tests ([#35](https://github.com/Arpan0995/pqc-migration-readiness/pull/35))
- [@lubineitor](https://github.com/lubineitor): migration-effort estimate logging in the Maven plugin ([#37](https://github.com/Arpan0995/pqc-migration-readiness/pull/37)) and a guard that rejects a truncated hybrid key-establishment wire with a clear error ([#67](https://github.com/Arpan0995/pqc-migration-readiness/pull/67))
- [@Voyagerroc-Lab](https://github.com/Voyagerroc-Lab): documented the Bouncy Castle 1.84 vs 1.86 version drift in the benchmark results ([#47](https://github.com/Arpan0995/pqc-migration-readiness/pull/47))
- [@Audgui-Byte](https://github.com/Audgui-Byte): scans now skip source files with invalid UTF-8 instead of aborting ([#50](https://github.com/Arpan0995/pqc-migration-readiness/pull/50)), and the agility audit log escapes JSON control characters ([#51](https://github.com/Arpan0995/pqc-migration-readiness/pull/51)), each with a regression test
- [@r-afael](https://github.com/r-afael): negative tests that reject incomplete dual signatures ([#52](https://github.com/Arpan0995/pqc-migration-readiness/pull/52)), and a run-level SARIF `columnKind` so `startColumn` is unambiguous ([#53](https://github.com/Arpan0995/pqc-migration-readiness/pull/53))
- [@headache1](https://github.com/headache1): a hard-negative scanner test that algorithm names in comments and string literals produce no findings ([#54](https://github.com/Arpan0995/pqc-migration-readiness/pull/54))

Contributions of any size are welcome, from a test fixture to a new detection
rule. See [CONTRIBUTING.md](CONTRIBUTING.md) to get started, and the open
[good first issues](https://github.com/Arpan0995/pqc-migration-readiness/labels/good%20first%20issue).
