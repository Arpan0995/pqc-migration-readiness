package org.pqcreadiness.agility;

import java.util.List;
import java.util.Optional;

/**
 * Selects a crypto suite for an intent given the local policy and a peer's capability
 * descriptor. The algorithm mirrors TLS/SSH negotiation (doc 06 &sect;4):
 *
 * <ol>
 *   <li>Choose the highest-preference suite in the local order that the peer also
 *       offers.</li>
 *   <li>If there is no intersection, apply the policy: fail closed, or downgrade to the
 *       strongest peer suite that still meets the floor.</li>
 * </ol>
 *
 * Selection is deterministic and in-process; exchanging the descriptors is the caller's
 * transport concern.
 */
public final class Negotiator {

    public NegotiationResult negotiate(Intent intent, IntentPolicy policy, CapabilityDescriptor peer) {
        List<String> peerOffer = peer.offered(intent);

        for (String id : policy.preferredSuiteIds()) {
            if (peerOffer.contains(id)) {
                return new NegotiationResult(Suites.require(id), false);
            }
        }

        if (policy.onNoIntersection() == IntentPolicy.OnNoIntersection.FAIL_CLOSED) {
            throw new NegotiationException(
                    "No common suite for " + intent + " and policy is fail-closed");
        }

        CryptoSuite best = null;
        for (String id : peerOffer) {
            Optional<CryptoSuite> lookup = Suites.byId(id);
            if (lookup.isEmpty()) {
                continue;
            }
            CryptoSuite suite = lookup.get();
            if (suite.intent() == intent && suite.mode().atLeast(policy.floor())
                    && (best == null || suite.mode().ordinal() > best.mode().ordinal())) {
                best = suite;
            }
        }
        if (best == null) {
            throw new NegotiationException(
                    "Downgrade found no suite at or above floor " + policy.floor() + " for " + intent);
        }
        return new NegotiationResult(best, true);
    }
}
