package org.pqcreadiness.agility;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of the crypto suites the agility layer knows about. The hybrid choices
 * follow current practice: X25519 + ML-KEM-768 for key establishment (the
 * X25519MLKEM768 pattern), and a dual ECDSA-P256 + ML-DSA-65 signature.
 */
public final class Suites {

    public static final CryptoSuite KE_HYBRID_X25519_MLKEM768 = new CryptoSuite(
            "KE-HYBRID-X25519-MLKEM768", Intent.KEY_ESTABLISHMENT, Mode.HYBRID, "X25519", "ML-KEM-768");
    public static final CryptoSuite KE_CLASSICAL_X25519 = new CryptoSuite(
            "KE-CLASSICAL-X25519", Intent.KEY_ESTABLISHMENT, Mode.CLASSICAL, "X25519", null);
    public static final CryptoSuite KE_PQC_MLKEM768 = new CryptoSuite(
            "KE-PQC-MLKEM768", Intent.KEY_ESTABLISHMENT, Mode.PQC_ONLY, null, "ML-KEM-768");

    public static final CryptoSuite SIG_DUAL_ECDSAP256_MLDSA65 = new CryptoSuite(
            "SIG-DUAL-ECDSAP256-MLDSA65", Intent.SIGNATURE, Mode.HYBRID, "SHA256withECDSA", "ML-DSA-65");
    public static final CryptoSuite SIG_CLASSICAL_ECDSAP256 = new CryptoSuite(
            "SIG-CLASSICAL-ECDSAP256", Intent.SIGNATURE, Mode.CLASSICAL, "SHA256withECDSA", null);
    public static final CryptoSuite SIG_MLDSA65 = new CryptoSuite(
            "SIG-MLDSA65", Intent.SIGNATURE, Mode.PQC_ONLY, null, "ML-DSA-65");

    private static final Map<String, CryptoSuite> BY_ID = new LinkedHashMap<>();

    static {
        for (CryptoSuite suite : new CryptoSuite[]{
                KE_HYBRID_X25519_MLKEM768, KE_CLASSICAL_X25519, KE_PQC_MLKEM768,
                SIG_DUAL_ECDSAP256_MLDSA65, SIG_CLASSICAL_ECDSAP256, SIG_MLDSA65}) {
            BY_ID.put(suite.id(), suite);
        }
    }

    private Suites() {
    }

    public static Optional<CryptoSuite> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** Look up a suite by ID, failing loudly for an unknown ID (a policy/config error). */
    public static CryptoSuite require(String id) {
        CryptoSuite suite = BY_ID.get(id);
        if (suite == null) {
            throw new IllegalArgumentException("Unknown crypto suite: " + id);
        }
        return suite;
    }
}
