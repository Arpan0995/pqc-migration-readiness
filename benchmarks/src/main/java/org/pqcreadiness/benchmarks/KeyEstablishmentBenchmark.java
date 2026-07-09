package org.pqcreadiness.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.pqcreadiness.agility.CryptoSuite;
import org.pqcreadiness.agility.Suites;
import org.pqcreadiness.agility.crypto.HybridKeyEstablishment;

import java.util.concurrent.TimeUnit;

/**
 * Key-establishment cost across postures: classical (X25519), hybrid
 * (X25519+ML-KEM-768), and PQC-only (ML-KEM-768). The headline the validation plan
 * wants is the relative overhead of hybrid/PQC over classical for keygen, encapsulate,
 * and decapsulate — the standard "abstraction is too slow" objection, answered with data.
 *
 * <p>Run from the module <em>classpath</em>, not the shaded jar — BC ships ML-KEM in a
 * multi-release jar that the shade plugin flattens, so the uber-jar loses ML-KEM
 * registration (see {@code benchmarks/results/RESULTS.md}). Results and interpretation
 * are in that file.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Benchmark)
public class KeyEstablishmentBenchmark {

    @Param({"KE-CLASSICAL-X25519", "KE-HYBRID-X25519-MLKEM768", "KE-PQC-MLKEM768"})
    public String suiteId;

    private HybridKeyEstablishment kem;
    private CryptoSuite suite;
    private HybridKeyEstablishment.RecipientKeys keys;
    private byte[] wire;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        kem = new HybridKeyEstablishment();
        suite = Suites.require(suiteId);
        keys = kem.generateRecipientKeys(suite);
        wire = kem.encapsulate(suite, keys).wire();
    }

    @Benchmark
    public Object keygen() throws Exception {
        return kem.generateRecipientKeys(suite);
    }

    @Benchmark
    public Object encapsulate() throws Exception {
        return kem.encapsulate(suite, keys);
    }

    @Benchmark
    public Object decapsulate() throws Exception {
        return kem.decapsulate(suite, keys, wire);
    }
}
