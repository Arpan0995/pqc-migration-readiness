package org.pqcreadiness.agility;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exhaustive-ish table tests over the negotiation matrix (mode x capability x policy). */
class NegotiatorTest {

    private final Negotiator negotiator = new Negotiator();

    private static CapabilityDescriptor peer(Intent intent, String... suiteIds) {
        return new CapabilityDescriptor(Map.of(intent, List.of(suiteIds)));
    }

    private static IntentPolicy policy(IntentPolicy.OnNoIntersection onNone, Mode floor, String... preferred) {
        return new IntentPolicy(Mode.HYBRID, List.of(preferred), onNone, floor);
    }

    @Test
    void picksHighestPreferenceCommonSuite() {
        IntentPolicy p = policy(IntentPolicy.OnNoIntersection.FAIL_CLOSED, Mode.CLASSICAL,
                Suites.KE_HYBRID_X25519_MLKEM768.id(), Suites.KE_CLASSICAL_X25519.id());
        CapabilityDescriptor peer = peer(Intent.KEY_ESTABLISHMENT,
                Suites.KE_CLASSICAL_X25519.id(), Suites.KE_HYBRID_X25519_MLKEM768.id());

        NegotiationResult r = negotiator.negotiate(Intent.KEY_ESTABLISHMENT, p, peer);

        assertEquals(Suites.KE_HYBRID_X25519_MLKEM768, r.suite());
        assertFalse(r.downgraded(), "a preferred-list match is not a downgrade");
    }

    @Test
    void preferenceFollowsLocalOrderNotPeerOrder() {
        // Local prefers classical first; even though peer lists hybrid first, local wins.
        IntentPolicy p = policy(IntentPolicy.OnNoIntersection.FAIL_CLOSED, Mode.CLASSICAL,
                Suites.KE_CLASSICAL_X25519.id(), Suites.KE_HYBRID_X25519_MLKEM768.id());
        CapabilityDescriptor peer = peer(Intent.KEY_ESTABLISHMENT,
                Suites.KE_HYBRID_X25519_MLKEM768.id(), Suites.KE_CLASSICAL_X25519.id());

        NegotiationResult r = negotiator.negotiate(Intent.KEY_ESTABLISHMENT, p, peer);

        assertEquals(Suites.KE_CLASSICAL_X25519, r.suite());
    }

    @Test
    void failsClosedWhenNoIntersection() {
        IntentPolicy p = policy(IntentPolicy.OnNoIntersection.FAIL_CLOSED, Mode.CLASSICAL,
                Suites.KE_HYBRID_X25519_MLKEM768.id());
        CapabilityDescriptor peer = peer(Intent.KEY_ESTABLISHMENT, Suites.KE_CLASSICAL_X25519.id());

        assertThrows(NegotiationException.class,
                () -> negotiator.negotiate(Intent.KEY_ESTABLISHMENT, p, peer));
    }

    @Test
    void downgradesToStrongestPeerSuiteMeetingFloor() {
        // Prefer PQC-only; peer offers only hybrid + classical; floor is classical.
        IntentPolicy p = policy(IntentPolicy.OnNoIntersection.DOWNGRADE, Mode.CLASSICAL,
                Suites.KE_PQC_MLKEM768.id());
        CapabilityDescriptor peer = peer(Intent.KEY_ESTABLISHMENT,
                Suites.KE_CLASSICAL_X25519.id(), Suites.KE_HYBRID_X25519_MLKEM768.id());

        NegotiationResult r = negotiator.negotiate(Intent.KEY_ESTABLISHMENT, p, peer);

        assertTrue(r.downgraded());
        assertEquals(Suites.KE_HYBRID_X25519_MLKEM768, r.suite(),
                "downgrade should pick hybrid over classical (strongest meeting floor)");
    }

    @Test
    void downgradeRespectsFloor() {
        // Prefer PQC-only, floor HYBRID; peer offers only classical -> nothing meets floor.
        IntentPolicy p = policy(IntentPolicy.OnNoIntersection.DOWNGRADE, Mode.HYBRID,
                Suites.KE_PQC_MLKEM768.id());
        CapabilityDescriptor peer = peer(Intent.KEY_ESTABLISHMENT, Suites.KE_CLASSICAL_X25519.id());

        assertThrows(NegotiationException.class,
                () -> negotiator.negotiate(Intent.KEY_ESTABLISHMENT, p, peer));
    }

    @Test
    void signatureIntentNegotiatesIndependently() {
        IntentPolicy p = policy(IntentPolicy.OnNoIntersection.FAIL_CLOSED, Mode.CLASSICAL,
                Suites.SIG_DUAL_ECDSAP256_MLDSA65.id(), Suites.SIG_CLASSICAL_ECDSAP256.id());
        CapabilityDescriptor peer = peer(Intent.SIGNATURE, Suites.SIG_MLDSA65.id(),
                Suites.SIG_DUAL_ECDSAP256_MLDSA65.id());

        NegotiationResult r = negotiator.negotiate(Intent.SIGNATURE, p, peer);

        assertEquals(Suites.SIG_DUAL_ECDSAP256_MLDSA65, r.suite());
    }
}
