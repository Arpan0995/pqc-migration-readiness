package org.pqcreadiness.auditor.rules;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Tables and predicates for quantum-vulnerable algorithm tokens, keyed by JCA
 * entry point. See {@code docs/research/02-detection-rule-catalog.md}.
 *
 * <p>JCA algorithm names are case-insensitive, so all lookups normalise to upper
 * case. For {@link javax.crypto.Cipher} transformations of the form
 * {@code "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"} only the token before the first
 * {@code '/'} is significant.
 */
public final class VulnerableAlgorithms {

    private VulnerableAlgorithms() {
    }

    /** Quantum-vulnerable algorithms for {@code KeyPairGenerator} / {@code KeyFactory}. */
    public static final Set<String> KEY_PAIR_GENERATOR = Set.of(
            "RSA", "RSASSA-PSS", "DSA", "DH", "DIFFIEHELLMAN",
            "EC", "ECDSA", "ECDH", "ECMQV",
            "X25519", "X448", "XDH", "ED25519", "ED448", "EDDSA");

    /** Quantum-vulnerable algorithms for {@code KeyAgreement}. */
    public static final Set<String> KEY_AGREEMENT = Set.of(
            "DH", "DIFFIEHELLMAN", "ECDH", "ECMQV", "X25519", "X448", "XDH");

    /** Quantum-vulnerable algorithm tokens for {@code Cipher.getInstance}. */
    public static final Set<String> CIPHER = Set.of(
            "RSA", "ECIES");

    /**
     * Concrete key interface types whose use in a type position couples code to a
     * quantum-vulnerable algorithm family (fragility indicator F4). Matched by
     * simple name against imports/usages.
     */
    public static final Set<String> CONCRETE_KEY_TYPES = Set.of(
            "RSAPublicKey", "RSAPrivateKey", "RSAPrivateCrtKey", "RSAKey",
            "DSAPublicKey", "DSAPrivateKey", "DSAKey",
            "ECPublicKey", "ECPrivateKey", "ECKey",
            "DHPublicKey", "DHPrivateKey", "DHKey",
            "EdECPublicKey", "EdECPrivateKey", "XECPublicKey", "XECPrivateKey");

    /** Normalise a raw algorithm string to its comparable token. */
    public static String normalize(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Extract the significant algorithm token from a {@code Cipher} transformation,
     * i.e. the part before the first {@code '/'}, upper-cased.
     */
    public static String cipherToken(String transformation) {
        String normalized = normalize(transformation);
        int slash = normalized.indexOf('/');
        return slash >= 0 ? normalized.substring(0, slash) : normalized;
    }

    /**
     * Classify a {@code Signature} algorithm string into a vulnerable family, if any.
     * Handles the composite {@code <digest>with<cipher>} form as well as the
     * standalone EdDSA and RSASSA-PSS names.
     *
     * @return the canonical family (RSA, ECDSA, DSA, ED25519, ED448, EDDSA), or empty
     */
    public static Optional<String> signatureFamily(String algorithm) {
        String a = normalize(algorithm);
        // Order matters: ECDSA must be tested before DSA.
        if (a.contains("WITHECDSA") || a.equals("ECDSA")) {
            return Optional.of("ECDSA");
        }
        if (a.contains("WITHRSA") || a.equals("RSASSA-PSS")) {
            return Optional.of("RSA");
        }
        if (a.contains("WITHDSA") || a.equals("DSA")) {
            return Optional.of("DSA");
        }
        if (a.equals("ED25519")) {
            return Optional.of("ED25519");
        }
        if (a.equals("ED448")) {
            return Optional.of("ED448");
        }
        if (a.equals("EDDSA")) {
            return Optional.of("EDDSA");
        }
        return Optional.empty();
    }
}
