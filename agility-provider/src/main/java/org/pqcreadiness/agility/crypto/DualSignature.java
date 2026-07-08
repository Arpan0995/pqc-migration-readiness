package org.pqcreadiness.agility.crypto;

import org.bouncycastle.jcajce.spec.MLDSAParameterSpec;
import org.pqcreadiness.agility.CryptoSuite;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.List;

/**
 * Signing across all three postures, delegating primitives to Bouncy Castle.
 *
 * <p>The hybrid suite produces a dual signature: sign with both ECDSA-P256 and
 * ML-DSA-65, and require <em>both</em> to verify (fail-closed AND semantics). The two
 * signatures are length-prefixed and concatenated with no clever composite encoding,
 * since composite-signature standardisation is still in flux. Classical and PQC-only
 * suites run just the corresponding half.
 */
public final class DualSignature {

    private static final String EC_CURVE = "secp256r1";

    /** Signer key material: a classical keypair and/or a PQC keypair per the suite. */
    public record SignerKeys(KeyPair classical, KeyPair pqc) {

        public PublicKey classicalPublic() {
            return classical == null ? null : classical.getPublic();
        }

        public PublicKey pqcPublic() {
            return pqc == null ? null : pqc.getPublic();
        }
    }

    public DualSignature() {
        Providers.ensureRegistered();
    }

    public SignerKeys generateSignerKeys(CryptoSuite suite) throws Exception {
        KeyPair classical = suite.hasClassical() ? newClassical() : null;
        KeyPair pqc = suite.hasPqc() ? newPqc(suite) : null;
        return new SignerKeys(classical, pqc);
    }

    public byte[] sign(CryptoSuite suite, SignerKeys keys, byte[] message) throws Exception {
        List<byte[]> blocks = new ArrayList<>();
        if (suite.hasClassical()) {
            blocks.add(signWith(suite.classicalAlgorithm(), keys.classical().getPrivate(), message));
        }
        if (suite.hasPqc()) {
            blocks.add(signWith("ML-DSA", keys.pqc().getPrivate(), message));
        }
        return Wire.concat(blocks);
    }

    /**
     * Verify a (possibly dual) signature. Every component the suite requires must be
     * present and valid; a missing or malformed block makes verification fail.
     */
    public boolean verify(CryptoSuite suite, SignerKeys keys, byte[] message, byte[] signature) throws Exception {
        List<byte[]> blocks = Wire.split(signature);
        int idx = 0;
        if (suite.hasClassical()) {
            if (idx >= blocks.size()
                    || !verifyWith(suite.classicalAlgorithm(), keys.classicalPublic(), message, blocks.get(idx++))) {
                return false;
            }
        }
        if (suite.hasPqc()) {
            if (idx >= blocks.size()
                    || !verifyWith("ML-DSA", keys.pqcPublic(), message, blocks.get(idx))) {
                return false;
            }
        }
        return true;
    }

    private static byte[] signWith(String algorithm, PrivateKey key, byte[] message) throws Exception {
        Signature sig = Signature.getInstance(algorithm, Providers.BC);
        sig.initSign(key);
        sig.update(message);
        return sig.sign();
    }

    private static boolean verifyWith(String algorithm, PublicKey key, byte[] message, byte[] bytes) throws Exception {
        Signature sig = Signature.getInstance(algorithm, Providers.BC);
        sig.initVerify(key);
        sig.update(message);
        return sig.verify(bytes);
    }

    private static KeyPair newClassical() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", Providers.BC);
        kpg.initialize(new ECGenParameterSpec(EC_CURVE));
        return kpg.generateKeyPair();
    }

    private static KeyPair newPqc(CryptoSuite suite) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA", Providers.BC);
        kpg.initialize(mldsaSpec(suite.pqcAlgorithm()));
        return kpg.generateKeyPair();
    }

    private static MLDSAParameterSpec mldsaSpec(String algorithm) {
        return switch (algorithm) {
            case "ML-DSA-44" -> MLDSAParameterSpec.ml_dsa_44;
            case "ML-DSA-65" -> MLDSAParameterSpec.ml_dsa_65;
            case "ML-DSA-87" -> MLDSAParameterSpec.ml_dsa_87;
            default -> throw new IllegalArgumentException("Unsupported ML-DSA parameter set: " + algorithm);
        };
    }
}
