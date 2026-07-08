package org.pqcreadiness.auditor.model;

/**
 * Functional category of a flagged crypto usage. Categories carry the base
 * difficulty weight applied by the scoring engine (see
 * {@code docs/research/03-difficulty-scoring-model.md}) and the urgency class
 * used to separate harvest-now-decrypt-later confidentiality risk from
 * signature (authenticity) risk.
 */
public enum Category {
    /** Asymmetric key establishment / encryption (Cipher-RSA, KeyAgreement, KEM). */
    KEY_ESTABLISHMENT(3, UrgencyClass.CONFIDENTIALITY),
    /** Digital signature sign or verify. */
    SIGNATURE(3, UrgencyClass.SIGNATURE),
    /** Key pair generation / key factory / algorithm-parameter construction. */
    KEYGEN(2, UrgencyClass.CONFIDENTIALITY),
    /** TLS configuration surface (enabled cipher suites, named groups, protocols). */
    TLS_CONFIG(2, UrgencyClass.CONFIDENTIALITY),
    /** JOSE / JWT algorithm pin. */
    JOSE(2, UrgencyClass.SIGNATURE),
    /** Coupling to a concrete key interface type (RSAPublicKey, ECPrivateKey, ...). */
    TYPE_COUPLING(1, UrgencyClass.CONFIDENTIALITY),
    /** Reported for completeness but never scored (AES-128, SHA-1, ...). */
    INFORMATIONAL(0, UrgencyClass.NONE);

    private final int baseWeight;
    private final UrgencyClass urgencyClass;

    Category(int baseWeight, UrgencyClass urgencyClass) {
        this.baseWeight = baseWeight;
        this.urgencyClass = urgencyClass;
    }

    /** Base difficulty weight before fragility multipliers (score model v0). */
    public int baseWeight() {
        return baseWeight;
    }

    public UrgencyClass urgencyClass() {
        return urgencyClass;
    }

    /** Whether findings in this category contribute to the difficulty score. */
    public boolean isScored() {
        return baseWeight > 0;
    }

    /** Urgency axis, kept orthogonal to difficulty so score validation stays clean. */
    public enum UrgencyClass {
        /** Harvest-now-decrypt-later: urgent now. */
        CONFIDENTIALITY,
        /** Fails only once a quantum computer exists at verification time. */
        SIGNATURE,
        /** Not applicable (informational findings). */
        NONE
    }
}
