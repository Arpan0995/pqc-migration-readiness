package org.pqcreadiness.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.TimeUnit;

/**
 * Placeholder benchmark used only to verify the JMH + shade plugin wiring
 * produces a runnable benchmark jar. Replace with real agility-layer
 * benchmarks once the agility provider has switchable crypto modes.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ScaffoldSanityBenchmark {

    @Benchmark
    public int baseline() {
        return 1 + 1;
    }
}
