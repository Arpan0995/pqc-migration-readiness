package org.pqcreadiness.agility;

/**
 * Cryptographic posture, ordered by post-quantum strength. The ordinal ordering
 * ({@code CLASSICAL < HYBRID < PQC_ONLY}) defines what "at or above a floor" means
 * during a policy-driven downgrade.
 */
public enum Mode {
    /** Classical algorithms only (RSA/ECDSA/ECDH/X25519). */
    CLASSICAL,
    /** Classical combined with a PQC algorithm; secure if either component holds. */
    HYBRID,
    /** PQC algorithms only. */
    PQC_ONLY;

    /** True if this mode is at least as post-quantum-strong as {@code floor}. */
    public boolean atLeast(Mode floor) {
        return this.ordinal() >= floor.ordinal();
    }
}
