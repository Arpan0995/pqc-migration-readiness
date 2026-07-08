package org.pqcreadiness.agility.crypto;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.jcajce.spec.MLKEMParameterSpec;
import org.pqcreadiness.agility.CryptoSuite;

import javax.crypto.KEM;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

/**
 * Key establishment across all three postures, delegating every primitive to Bouncy
 * Castle; only the <em>combination</em> logic is ours.
 *
 * <p>For the hybrid suite the classical component is an ephemeral X25519 ECDH and the
 * PQC component is ML-KEM-768 encapsulation. The shared secret is
 * {@code HKDF-SHA256(ss_classical || ss_pqc)} — the construction standardised by
 * X25519MLKEM768 — so the result is secure if <em>either</em> component holds. Classical
 * and PQC-only suites run just the corresponding half.
 */
public final class HybridKeyEstablishment {

    private static final int SECRET_LEN = 32;
    private static final byte[] HKDF_INFO = "pqc-agility/hybrid-kem/v1".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    /** Recipient key material: a classical keypair and/or a PQC keypair per the suite. */
    public record RecipientKeys(KeyPair classical, KeyPair pqc) {
    }

    /** Result of encapsulation: the derived secret and the wire bytes the peer needs. */
    public record Encapsulation(byte[] sharedSecret, byte[] wire) {
    }

    public HybridKeyEstablishment() {
        Providers.ensureRegistered();
    }

    public RecipientKeys generateRecipientKeys(CryptoSuite suite) throws Exception {
        KeyPair classical = suite.hasClassical() ? newClassical(suite) : null;
        KeyPair pqc = suite.hasPqc() ? newPqc(suite) : null;
        return new RecipientKeys(classical, pqc);
    }

    public Encapsulation encapsulate(CryptoSuite suite, RecipientKeys recipient) throws Exception {
        List<byte[]> secrets = new ArrayList<>();
        List<byte[]> wireBlocks = new ArrayList<>();

        if (suite.hasClassical()) {
            KeyPair ephemeral = newClassical(suite);
            KeyAgreement ka = KeyAgreement.getInstance(suite.classicalAlgorithm(), Providers.BC);
            ka.init(ephemeral.getPrivate());
            ka.doPhase(recipient.classical().getPublic(), true);
            secrets.add(ka.generateSecret());
            wireBlocks.add(ephemeral.getPublic().getEncoded());
        }
        if (suite.hasPqc()) {
            KEM kem = KEM.getInstance("ML-KEM", Providers.BC);
            KEM.Encapsulator enc = kem.newEncapsulator(recipient.pqc().getPublic());
            KEM.Encapsulated e = enc.encapsulate();
            secrets.add(e.key().getEncoded());
            wireBlocks.add(e.encapsulation());
        }
        return new Encapsulation(combine(secrets), Wire.concat(wireBlocks));
    }

    public byte[] decapsulate(CryptoSuite suite, RecipientKeys recipient, byte[] wire) throws Exception {
        List<byte[]> blocks = Wire.split(wire);
        List<byte[]> secrets = new ArrayList<>();
        int idx = 0;

        if (suite.hasClassical()) {
            byte[] ephemeralPubEncoded = blocks.get(idx++);
            PublicKey ephemeralPub = KeyFactory.getInstance(suite.classicalAlgorithm(), Providers.BC)
                    .generatePublic(new X509EncodedKeySpec(ephemeralPubEncoded));
            KeyAgreement ka = KeyAgreement.getInstance(suite.classicalAlgorithm(), Providers.BC);
            ka.init(recipient.classical().getPrivate());
            ka.doPhase(ephemeralPub, true);
            secrets.add(ka.generateSecret());
        }
        if (suite.hasPqc()) {
            byte[] ciphertext = blocks.get(idx);
            KEM kem = KEM.getInstance("ML-KEM", Providers.BC);
            KEM.Decapsulator dec = kem.newDecapsulator(recipient.pqc().getPrivate());
            SecretKey ss = dec.decapsulate(ciphertext);
            secrets.add(ss.getEncoded());
        }
        return combine(secrets);
    }

    private static KeyPair newClassical(CryptoSuite suite) throws Exception {
        return KeyPairGenerator.getInstance(suite.classicalAlgorithm(), Providers.BC).generateKeyPair();
    }

    private static KeyPair newPqc(CryptoSuite suite) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-KEM", Providers.BC);
        kpg.initialize(mlkemSpec(suite.pqcAlgorithm()));
        return kpg.generateKeyPair();
    }

    private static MLKEMParameterSpec mlkemSpec(String algorithm) {
        return switch (algorithm) {
            case "ML-KEM-512" -> MLKEMParameterSpec.ml_kem_512;
            case "ML-KEM-768" -> MLKEMParameterSpec.ml_kem_768;
            case "ML-KEM-1024" -> MLKEMParameterSpec.ml_kem_1024;
            default -> throw new IllegalArgumentException("Unsupported ML-KEM parameter set: " + algorithm);
        };
    }

    /** HKDF-SHA256 over the concatenated component secrets. */
    private static byte[] combine(List<byte[]> secrets) {
        ByteArrayOutputStream ikm = new ByteArrayOutputStream();
        for (byte[] s : secrets) {
            ikm.writeBytes(s);
        }
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(ikm.toByteArray(), null, HKDF_INFO));
        byte[] out = new byte[SECRET_LEN];
        hkdf.generateBytes(out, 0, out.length);
        return out;
    }

}
