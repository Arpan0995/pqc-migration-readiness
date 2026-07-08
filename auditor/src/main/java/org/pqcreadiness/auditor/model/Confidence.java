package org.pqcreadiness.auditor.model;

/**
 * How certain the auditor is that a finding is a genuine quantum-vulnerable usage.
 *
 * <p>Confidence is reported per finding and drives calibration analysis (see
 * {@code docs/research/05-validation-and-benchmark-plan.md}): HIGH findings are
 * expected to have near-1.0 precision, while LOW findings are allowed to be noisy
 * because they mainly exist to surface unresolved algorithm selection (an agility
 * signal), not to raise alarms.
 */
public enum Confidence {
    /** Algorithm resolved from a string literal at the call site. */
    HIGH,
    /** Algorithm resolved by propagating a constant (e.g. a static final field). */
    MEDIUM,
    /** Algorithm supplied by a dynamic expression that could not be resolved. */
    LOW
}
