package org.pqcreadiness.agility.audit;

/**
 * One audit record for a cryptographic operation performed through the agility layer.
 *
 * <p>Audit logging is a first-class requirement of crypto agility (NIST CSWP 39): it
 * provides the evidence trail an organisation needs to prove it stopped using
 * classical-only crypto by a given date, and it cross-checks the benchmark harness.
 *
 * @param timestamp     ISO-8601 event time
 * @param intent        the intent served
 * @param mode          the posture actually used
 * @param suiteId       the selected suite ID
 * @param peerOffer     comma-joined peer offer for this intent (or empty)
 * @param outcome       {@code SELECTED}, {@code DOWNGRADED}, or {@code FAILED}
 * @param durationNanos wall-clock duration of the operation, if measured (else -1)
 */
public record AuditEvent(
        String timestamp,
        String intent,
        String mode,
        String suiteId,
        String peerOffer,
        String outcome,
        long durationNanos) {
}
