package org.pqcreadiness.agility.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

/**
 * Registers the Bouncy Castle provider exactly once. On JDK 21 the JCA has no built-in
 * ML-KEM/ML-DSA, so BC supplies the PQC primitives; the agility layer treats the
 * provider as a policy field so a later move to JDK-native algorithms (25 LTS+) is a
 * configuration change rather than a code change.
 */
public final class Providers {

    /** JCA provider name used for all {@code getInstance} calls in this module. */
    public static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    private static volatile boolean registered;

    private Providers() {
    }

    public static synchronized void ensureRegistered() {
        if (!registered) {
            if (Security.getProvider(BC) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            registered = true;
        }
    }
}
