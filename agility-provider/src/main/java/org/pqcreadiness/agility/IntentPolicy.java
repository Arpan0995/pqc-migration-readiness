package org.pqcreadiness.agility;

import java.util.List;

/**
 * Policy for a single {@link Intent}: the preferred posture, the ordered suite IDs the
 * local party offers (its own capability, in preference order), what to do when there
 * is no intersection with a peer, and the minimum posture acceptable on downgrade.
 *
 * @param mode              the preferred posture (informational; the suite list encodes the real choice)
 * @param preferredSuiteIds local offer, most preferred first
 * @param onNoIntersection  behaviour when the preferred list does not intersect the peer's offer
 * @param floor             minimum posture acceptable when downgrading
 */
public record IntentPolicy(
        Mode mode,
        List<String> preferredSuiteIds,
        OnNoIntersection onNoIntersection,
        Mode floor) {

    public IntentPolicy {
        preferredSuiteIds = List.copyOf(preferredSuiteIds);
    }

    /** Behaviour when a peer shares none of our preferred suites. */
    public enum OnNoIntersection {
        /** Refuse to proceed (secure default). */
        FAIL_CLOSED,
        /** Accept the strongest peer suite that still meets {@link IntentPolicy#floor}. */
        DOWNGRADE
    }

    /** The local party's capability descriptor entry derived from this policy. */
    public List<String> asOffer() {
        return preferredSuiteIds;
    }
}
