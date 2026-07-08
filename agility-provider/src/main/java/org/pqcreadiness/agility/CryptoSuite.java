package org.pqcreadiness.agility;

/**
 * A named, versioned algorithm tuple. Suite IDs are the single vocabulary shared by
 * policy, capability negotiation, and audit logs, so the same string that appears in
 * configuration also appears in a peer's offer and in the audit record.
 *
 * @param id                 stable suite identifier, e.g. {@code KE-HYBRID-X25519-MLKEM768}
 * @param intent             the intent this suite serves
 * @param mode               the posture this suite provides
 * @param classicalAlgorithm JCA algorithm name of the classical component, or {@code null}
 * @param pqcAlgorithm       algorithm name of the PQC component, or {@code null}
 */
public record CryptoSuite(
        String id,
        Intent intent,
        Mode mode,
        String classicalAlgorithm,
        String pqcAlgorithm) {

    public boolean hasClassical() {
        return classicalAlgorithm != null;
    }

    public boolean hasPqc() {
        return pqcAlgorithm != null;
    }
}
