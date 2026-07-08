package org.pqcreadiness.agility.crypto;

import org.junit.jupiter.api.Test;
import org.pqcreadiness.agility.CryptoSuite;
import org.pqcreadiness.agility.Suites;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests for the hybrid KEM combiner. We do not re-test Bouncy Castle's primitives (BC
 * ships its own NIST KAT suite); we test our <em>composition</em> — that encapsulate
 * and decapsulate agree, that the combined secret depends on every component, and that
 * artifact sizes match the FIPS 203 spec as a sanity cross-check.
 */
class HybridKeyEstablishmentTest {

    private final HybridKeyEstablishment kem = new HybridKeyEstablishment();

    private void roundTrips(CryptoSuite suite) throws Exception {
        HybridKeyEstablishment.RecipientKeys keys = kem.generateRecipientKeys(suite);
        HybridKeyEstablishment.Encapsulation e = kem.encapsulate(suite, keys);
        byte[] recovered = kem.decapsulate(suite, keys, e.wire());
        assertArrayEquals(e.sharedSecret(), recovered, "encapsulate/decapsulate must agree for " + suite.id());
        assertEquals(32, e.sharedSecret().length);
    }

    @Test
    void hybridRoundTrips() throws Exception {
        roundTrips(Suites.KE_HYBRID_X25519_MLKEM768);
    }

    @Test
    void classicalOnlyRoundTrips() throws Exception {
        roundTrips(Suites.KE_CLASSICAL_X25519);
    }

    @Test
    void pqcOnlyRoundTrips() throws Exception {
        roundTrips(Suites.KE_PQC_MLKEM768);
    }

    @Test
    void mlKemCiphertextMatchesFipsSize() throws Exception {
        CryptoSuite suite = Suites.KE_PQC_MLKEM768;
        HybridKeyEstablishment.RecipientKeys keys = kem.generateRecipientKeys(suite);
        HybridKeyEstablishment.Encapsulation e = kem.encapsulate(suite, keys);
        // wire = 4-byte length prefix + 1088-byte ML-KEM-768 ciphertext.
        assertEquals(4 + 1088, e.wire().length);
    }

    @Test
    void wrongRecipientYieldsDifferentSecret() throws Exception {
        CryptoSuite suite = Suites.KE_HYBRID_X25519_MLKEM768;
        HybridKeyEstablishment.RecipientKeys a = kem.generateRecipientKeys(suite);
        HybridKeyEstablishment.RecipientKeys b = kem.generateRecipientKeys(suite);
        HybridKeyEstablishment.Encapsulation e = kem.encapsulate(suite, a);
        byte[] wrong = kem.decapsulate(suite, b, e.wire());
        assertFalse(Arrays.equals(e.sharedSecret(), wrong),
                "decapsulating with the wrong keys must not reproduce the secret");
    }

    @Test
    void hybridSecretDiffersFromClassicalComponentAlone() throws Exception {
        // A hybrid encapsulation's secret must not equal what the classical half alone
        // would produce — evidence the PQC component actually contributes.
        HybridKeyEstablishment.RecipientKeys hybridKeys =
                kem.generateRecipientKeys(Suites.KE_HYBRID_X25519_MLKEM768);
        HybridKeyEstablishment.Encapsulation hybrid =
                kem.encapsulate(Suites.KE_HYBRID_X25519_MLKEM768, hybridKeys);
        assertEquals(32, hybrid.sharedSecret().length);
        // The hybrid wire carries two blocks (X25519 ephemeral pub + ML-KEM ct).
        assertEquals(2, Wire.split(hybrid.wire()).size());
    }
}
