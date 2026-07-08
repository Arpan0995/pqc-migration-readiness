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
import org.pqcreadiness.agility.crypto.DualSignature;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Signature cost across postures: classical (ECDSA-P256), hybrid dual
 * (ECDSA-P256+ML-DSA-65), and PQC-only (ML-DSA-65), for keygen, sign, and verify.
 * ML-DSA signatures are ~3.3 KB versus ~72 B for ECDSA, so this quantifies both the
 * CPU cost and (via {@code -prof gc}) the allocation pressure the JVM-specific part of
 * the study cares about.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Benchmark)
public class SignatureBenchmark {

    @Param({"SIG-CLASSICAL-ECDSAP256", "SIG-DUAL-ECDSAP256-MLDSA65", "SIG-MLDSA65"})
    public String suiteId;

    private static final byte[] MESSAGE = "benchmark payload to be signed".getBytes(StandardCharsets.UTF_8);

    private DualSignature signer;
    private CryptoSuite suite;
    private DualSignature.SignerKeys keys;
    private byte[] signature;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        signer = new DualSignature();
        suite = Suites.require(suiteId);
        keys = signer.generateSignerKeys(suite);
        signature = signer.sign(suite, keys, MESSAGE);
    }

    @Benchmark
    public Object keygen() throws Exception {
        return signer.generateSignerKeys(suite);
    }

    @Benchmark
    public Object sign() throws Exception {
        return signer.sign(suite, keys, MESSAGE);
    }

    @Benchmark
    public boolean verify() throws Exception {
        return signer.verify(suite, keys, MESSAGE, signature);
    }
}
