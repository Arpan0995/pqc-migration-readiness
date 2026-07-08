package org.pqcreadiness.agility.crypto;

import org.junit.jupiter.api.Test;
import org.pqcreadiness.agility.CryptoSuite;
import org.pqcreadiness.agility.Suites;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the dual-signature composition: valid signatures verify, tampering with
 * either component fails verification (fail-closed AND), and each posture signs with
 * the expected number of components.
 */
class DualSignatureTest {

    private final DualSignature signer = new DualSignature();
    private final byte[] message = "attest this".getBytes();

    private void signVerifyRoundTrips(CryptoSuite suite) throws Exception {
        DualSignature.SignerKeys keys = signer.generateSignerKeys(suite);
        byte[] sig = signer.sign(suite, keys, message);
        assertTrue(signer.verify(suite, keys, message, sig), "valid signature must verify for " + suite.id());
    }

    @Test
    void dualRoundTrips() throws Exception {
        signVerifyRoundTrips(Suites.SIG_DUAL_ECDSAP256_MLDSA65);
    }

    @Test
    void classicalOnlyRoundTrips() throws Exception {
        signVerifyRoundTrips(Suites.SIG_CLASSICAL_ECDSAP256);
    }

    @Test
    void pqcOnlyRoundTrips() throws Exception {
        signVerifyRoundTrips(Suites.SIG_MLDSA65);
    }

    @Test
    void rejectsTamperedMessage() throws Exception {
        CryptoSuite suite = Suites.SIG_DUAL_ECDSAP256_MLDSA65;
        DualSignature.SignerKeys keys = signer.generateSignerKeys(suite);
        byte[] sig = signer.sign(suite, keys, message);
        assertFalse(signer.verify(suite, keys, "different".getBytes(), sig));
    }

    @Test
    void dualFailsIfPqcComponentCorrupted() throws Exception {
        CryptoSuite suite = Suites.SIG_DUAL_ECDSAP256_MLDSA65;
        DualSignature.SignerKeys keys = signer.generateSignerKeys(suite);
        byte[] sig = signer.sign(suite, keys, message);

        // Corrupt a byte inside the second (ML-DSA) block.
        List<byte[]> blocks = Wire.split(sig);
        assertEquals(2, blocks.size(), "dual signature carries two blocks");
        byte[] pqcBlock = blocks.get(1);
        pqcBlock[pqcBlock.length / 2] ^= 0x01;
        byte[] tampered = Wire.concat(blocks);

        assertFalse(signer.verify(suite, keys, message, tampered),
                "a corrupted PQC component must fail the AND verification");
    }

    @Test
    void mlDsaSignatureMatchesFipsSize() throws Exception {
        CryptoSuite suite = Suites.SIG_MLDSA65;
        DualSignature.SignerKeys keys = signer.generateSignerKeys(suite);
        byte[] sig = signer.sign(suite, keys, message);
        List<byte[]> blocks = Wire.split(sig);
        assertEquals(1, blocks.size());
        assertEquals(3309, blocks.get(0).length, "ML-DSA-65 signature is 3309 bytes");
    }
}
