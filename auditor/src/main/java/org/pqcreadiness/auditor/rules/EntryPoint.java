package org.pqcreadiness.auditor.rules;

import org.pqcreadiness.auditor.model.Category;

import java.util.Optional;
import java.util.Set;

/**
 * A JCA factory entry point whose {@code getInstance(algorithm)} calls the auditor
 * inspects. Each entry point knows how to decide whether an algorithm string is
 * quantum-vulnerable and, if so, which canonical family token and rule ID to report.
 *
 * <p>Matching is by the receiver's trailing type name (e.g. {@code Cipher}), which
 * keeps detection purely syntactic and therefore robust on codebases scanned without
 * a resolved classpath.
 */
public enum EntryPoint {

    CIPHER("Cipher", Category.KEY_ESTABLISHMENT, "JCA-CIPHER") {
        @Override
        public Optional<String> match(String algorithm) {
            String token = VulnerableAlgorithms.cipherToken(algorithm);
            return VulnerableAlgorithms.CIPHER.contains(token) ? Optional.of(token) : Optional.empty();
        }
    },
    KEY_PAIR_GENERATOR("KeyPairGenerator", Category.KEYGEN, "JCA-KPG") {
        @Override
        public Optional<String> match(String algorithm) {
            return inSet(algorithm, VulnerableAlgorithms.KEY_PAIR_GENERATOR);
        }
    },
    KEY_FACTORY("KeyFactory", Category.KEYGEN, "JCA-KF") {
        @Override
        public Optional<String> match(String algorithm) {
            return inSet(algorithm, VulnerableAlgorithms.KEY_PAIR_GENERATOR);
        }
    },
    KEY_AGREEMENT("KeyAgreement", Category.KEY_ESTABLISHMENT, "JCA-KA") {
        @Override
        public Optional<String> match(String algorithm) {
            return inSet(algorithm, VulnerableAlgorithms.KEY_AGREEMENT);
        }
    },
    SIGNATURE("Signature", Category.SIGNATURE, "JCA-SIG") {
        @Override
        public Optional<String> match(String algorithm) {
            return VulnerableAlgorithms.signatureFamily(algorithm);
        }
    };

    private final String receiverType;
    private final Category category;
    private final String ruleIdPrefix;

    EntryPoint(String receiverType, Category category, String ruleIdPrefix) {
        this.receiverType = receiverType;
        this.category = category;
        this.ruleIdPrefix = ruleIdPrefix;
    }

    /** Trailing type name of the receiver this entry point matches, e.g. {@code Cipher}. */
    public String receiverType() {
        return receiverType;
    }

    public Category category() {
        return category;
    }

    /** Build the rule ID for a matched family token, e.g. {@code JCA-KPG-RSA}. */
    public String ruleId(String familyToken) {
        return ruleIdPrefix + "-" + familyToken;
    }

    /**
     * If {@code algorithm} is quantum-vulnerable for this entry point, return its
     * canonical family token; otherwise empty.
     */
    public abstract Optional<String> match(String algorithm);

    /** Look up an entry point by the receiver's trailing type name. */
    public static Optional<EntryPoint> forReceiver(String receiverType) {
        for (EntryPoint ep : values()) {
            if (ep.receiverType.equals(receiverType)) {
                return Optional.of(ep);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> inSet(String algorithm, Set<String> table) {
        String token = VulnerableAlgorithms.normalize(algorithm);
        return table.contains(token) ? Optional.of(token) : Optional.empty();
    }
}
