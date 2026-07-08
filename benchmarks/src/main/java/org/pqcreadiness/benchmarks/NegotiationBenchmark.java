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
import org.pqcreadiness.agility.CapabilityDescriptor;
import org.pqcreadiness.agility.Intent;
import org.pqcreadiness.agility.IntentPolicy;
import org.pqcreadiness.agility.NegotiationResult;
import org.pqcreadiness.agility.Negotiator;
import org.pqcreadiness.agility.Suites;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Capability-negotiation overhead. This should be nanoseconds — negligible next to the
 * crypto operations — but it is reported separately so the agility layer's total cost is
 * fully accounted for. The scenarios mirror the peer cases in the validation plan:
 * hybrid-capable peer (preferred match) versus classical-only peer (forces a downgrade).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Benchmark)
public class NegotiationBenchmark {

    /** Peer posture: does the peer support our preferred hybrid suite, or only classical? */
    @Param({"HYBRID_CAPABLE", "CLASSICAL_ONLY"})
    public String peerScenario;

    private final Negotiator negotiator = new Negotiator();
    private IntentPolicy policy;
    private CapabilityDescriptor peer;

    @Setup(Level.Trial)
    public void setup() {
        policy = new IntentPolicy(
                org.pqcreadiness.agility.Mode.HYBRID,
                List.of(Suites.KE_HYBRID_X25519_MLKEM768.id(), Suites.KE_CLASSICAL_X25519.id()),
                IntentPolicy.OnNoIntersection.DOWNGRADE,
                org.pqcreadiness.agility.Mode.CLASSICAL);
        List<String> offer = peerScenario.equals("HYBRID_CAPABLE")
                ? List.of(Suites.KE_HYBRID_X25519_MLKEM768.id(), Suites.KE_CLASSICAL_X25519.id())
                : List.of(Suites.KE_CLASSICAL_X25519.id());
        peer = new CapabilityDescriptor(Map.of(Intent.KEY_ESTABLISHMENT, offer));
    }

    @Benchmark
    public NegotiationResult negotiate() {
        return negotiator.negotiate(Intent.KEY_ESTABLISHMENT, policy, peer);
    }
}
