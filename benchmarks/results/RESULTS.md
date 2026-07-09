# Agility-Layer Benchmark Results

Runtime cost of the crypto-agility layer across classical / hybrid / PQC-only postures.
This is the [doc 05 §4](../../docs/research/05-validation-and-benchmark-plan.md) benchmark
deliverable — it answers the standard *"an abstraction layer / PQC is too slow"* objection
with measured data, and quantifies the JVM-specific allocation effect the project set out
to study.

## Environment

- **Hardware:** Apple M2, 8 cores
- **JVM:** OpenJDK 21.0.11 (Homebrew)
- **Crypto:** Bouncy Castle `bcprov-jdk18on:1.84` (ML-KEM / ML-DSA)
- **JMH:** 1.37 — `@Fork(2)`, `@Warmup(5×1s)`, `@Measurement(5×1s)` (Cnt = 10 per row),
  `AverageTime`, `-prof gc`
- **Date:** 2026-07-09
- Raw data: [`jmh-results.json`](jmh-results.json) (latency) and
  [`jmh-run.log`](jmh-run.log) (full output incl. per-iteration GC).

> **Methodology note (reproducibility).** These must be run from the module **classpath**,
> not the shaded `benchmarks.jar`. BC ships ML-KEM in a multi-release jar
> (`META-INF/versions/9/…`); the shade plugin flattens that, so the uber-jar's BC provider
> silently loses ML-KEM registration and the hybrid/PQC KEM benchmarks fail with
> `NoSuchAlgorithmException: no such algorithm: ML-KEM`. Run instead with:
> ```
> mvn -pl benchmarks dependency:build-classpath -Dmdep.outputFile=target/bench-cp.txt
> java -cp "benchmarks/target/classes:$(cat benchmarks/target/bench-cp.txt)" \
>     org.openjdk.jmh.Main -prof gc -rf json -rff benchmarks/results/jmh-results.json
> ```

## Key establishment (baseline = classical X25519)

| Op | Suite | Latency | ×base | Alloc B/op | ×base |
|---|---|--:|--:|--:|--:|
| keygen | X25519 (classical) | 43.6 µs | — | 3,720 | — |
| keygen | X25519+ML-KEM-768 (hybrid) | 102.4 µs | 2.3× | 47,073 | 12.7× |
| keygen | ML-KEM-768 (PQC) | 55.6 µs | 1.3× | 43,321 | 11.6× |
| encapsulate | X25519 (classical) | 108.0 µs | — | 8,413 | — |
| encapsulate | X25519+ML-KEM-768 (hybrid) | 138.2 µs | 1.3× | 36,713 | 4.4× |
| encapsulate | ML-KEM-768 (PQC) | 36.1 µs | **0.3×** | 31,048 | 3.7× |
| decapsulate | X25519 (classical) | 62.3 µs | — | 4,737 | — |
| decapsulate | X25519+ML-KEM-768 (hybrid) | 101.2 µs | 1.6× | 34,597 | 7.3× |
| decapsulate | ML-KEM-768 (PQC) | 42.4 µs | **0.7×** | 32,460 | 6.9× |

## Signatures (baseline = classical ECDSA-P256)

| Op | Suite | Latency | ×base | Alloc B/op | ×base |
|---|---|--:|--:|--:|--:|
| keygen | ECDSA-P256 (classical) | 66.9 µs | — | 52,389 | — |
| keygen | ECDSA+ML-DSA-65 (dual) | 187.7 µs | 2.8× | 189,691 | 3.6× |
| keygen | ML-DSA-65 (PQC) | 120.4 µs | 1.8× | 137,257 | 2.6× |
| sign | ECDSA-P256 (classical) | 65.6 µs | — | 52,633 | — |
| sign | ECDSA+ML-DSA-65 (dual) | 355.9 µs | 5.4× | 266,227 | 5.1× |
| sign | ML-DSA-65 (PQC) | 513.7 µs | **7.8×** | 341,908 | 6.5× |
| verify | ECDSA-P256 (classical) | 72.3 µs | — | 81,780 | — |
| verify | ECDSA+ML-DSA-65 (dual) | 171.7 µs | 2.4× | 207,014 | 2.5× |
| verify | ML-DSA-65 (PQC) | 119.9 µs | 1.7× | 125,265 | 1.5× |

## Negotiation overhead

| Peer scenario | Latency | Alloc B/op |
|---|--:|--:|
| Hybrid-capable (preferred match) | 6.44 ns | 24 |
| Classical-only (forces downgrade) | 8.98 ns | 24 |

## Interpretation

**The agility layer's own overhead is negligible.** Capability negotiation costs
**~6–9 nanoseconds** and 24 bytes — roughly seven orders of magnitude below the crypto
operations it selects (µs–ms). The "abstraction layer is too expensive" objection does not
survive contact with the data; whatever cost exists is the *algorithms*, not the policy/
negotiation wrapper around them.

**PQC is not uniformly "slower" — it is different per operation.** ML-KEM
**encapsulate/decapsulate are actually faster than** classical X25519 (0.3× and 0.7×),
because ML-KEM's lattice arithmetic is cheaper than an X25519 scalar multiplication; the
cost moves to keygen. So a KEM-heavy workload can get *faster* moving to PQC. Signatures
are where the real CPU cost lands: **ML-DSA signing is ~7.8× ECDSA** (dual-sign 5.4×),
though verification stays modest (1.5–1.7×).

**The JVM-specific story is allocation, not just latency** — the effect this project set
out to surface. Hybrid/PQC operations allocate **4–13× more per operation** (hybrid KE
keygen 47 KB/op vs 3.7 KB/op classical; ML-DSA sign 342 KB/op vs 53 KB/op), driven by the
multi-KB keys, ciphertexts, and signatures. For a service doing many handshakes/signatures
per second this is real, sustained GC pressure that a C/Rust deployment would not see the
same way — the under-studied JVM angle the C/Rust-heavy PQC literature misses. Hybrid is
consistently the heaviest (it pays both components plus the HKDF combine), which is the
honest cost of "secure if either component holds."

## Caveats

- Microbenchmarks on one machine (Apple M2); absolute numbers are platform-specific — read
  the **ratios**, not the µs. Reproduce on target hardware before quoting figures.
- BC 1.84 software implementations; a different provider or JDK-native ML-KEM/ML-DSA
  (JDK 24+) would shift absolute costs.
- Dual-signature keygen/sign do both algorithms sequentially; a parallel implementation
  would cut the hybrid signing latency (not the allocation).
- SLH-DSA (much larger, slower signatures) is not in this matrix; add it if hash-based
  signatures become a target posture.
