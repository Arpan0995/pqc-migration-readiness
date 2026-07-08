package org.pqcreadiness.auditor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffortTierTest {

    @Test
    void zeroScoreIsNone() {
        assertEquals(EffortTier.NONE, EffortTier.forScore(0.0));
    }

    @Test
    void smallPositiveScoreIsLow() {
        assertEquals(EffortTier.LOW, EffortTier.forScore(0.5));
        assertEquals(EffortTier.LOW, EffortTier.forScore(9.99));
    }

    @Test
    void boundaryScoresRoundUpToNextTier() {
        assertEquals(EffortTier.MEDIUM, EffortTier.forScore(10.0));
        assertEquals(EffortTier.HIGH, EffortTier.forScore(40.0));
        assertEquals(EffortTier.CRITICAL, EffortTier.forScore(120.0));
    }

    @Test
    void largeScoreIsCritical() {
        assertEquals(EffortTier.CRITICAL, EffortTier.forScore(5000.0));
    }

    @Test
    void tiersAreMonotonicWithScore() {
        double[] scores = {0, 1, 9, 10, 39, 40, 119, 120, 1000};
        EffortTier previous = EffortTier.NONE;
        for (double score : scores) {
            EffortTier tier = EffortTier.forScore(score);
            assertTrue(tier.ordinal() >= previous.ordinal(),
                    "tier must not decrease as score increases: " + score + " -> " + tier);
            previous = tier;
        }
    }
}
