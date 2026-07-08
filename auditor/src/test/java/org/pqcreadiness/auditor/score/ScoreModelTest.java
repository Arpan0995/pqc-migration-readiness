package org.pqcreadiness.auditor.score;

import org.junit.jupiter.api.Test;
import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.Confidence;
import org.pqcreadiness.auditor.model.Finding;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for the pre-registered score model v0 (doc 03). */
class ScoreModelTest {

    private static Finding finding(Category category, List<String> fragility) {
        return new Finding("RULE", "F.java", 1, 1, "api", "RSA", category,
                Confidence.HIGH, fragility, "snippet");
    }

    @Test
    void baseWeightsWithoutFragility() {
        assertEquals(3.0, ScoreModel.findingDifficulty(finding(Category.KEY_ESTABLISHMENT, List.of())));
        assertEquals(3.0, ScoreModel.findingDifficulty(finding(Category.SIGNATURE, List.of())));
        assertEquals(2.0, ScoreModel.findingDifficulty(finding(Category.KEYGEN, List.of())));
    }

    @Test
    void informationalNeverScores() {
        assertEquals(0.0, ScoreModel.findingDifficulty(finding(Category.INFORMATIONAL, List.of("F1"))));
    }

    @Test
    void singleFragilityMultiplier() {
        // KEYGEN base 2 * F1 (1.5) = 3.0
        assertEquals(3.0, ScoreModel.findingDifficulty(finding(Category.KEYGEN, List.of("F1"))));
    }

    @Test
    void agilityCreditReducesDifficulty() {
        // KEYGEN base 2 * F5 (0.5) = 1.0
        assertEquals(1.0, ScoreModel.findingDifficulty(finding(Category.KEYGEN, List.of("F5"))));
    }

    @Test
    void multiplierProductIsCapped() {
        // F8(2.5) * F2(2.0) * F1(1.5) = 7.5 -> capped at 6.0; base 3 -> 18.0
        double d = ScoreModel.findingDifficulty(
                finding(Category.KEY_ESTABLISHMENT, List.of("F8", "F2", "F1")));
        assertEquals(18.0, d);
    }

    @Test
    void typeCouplingDoesNotMultiplyItsOwnF4() {
        // TYPE_COUPLING base 1, its definitional F4 is not an extra multiplier -> 1.0
        assertEquals(1.0, ScoreModel.findingDifficulty(finding(Category.TYPE_COUPLING, List.of("F4"))));
    }

    @Test
    void spreadGrowsWithFileCount() {
        assertEquals(1.0, ScoreModel.spread(0));
        assertEquals(1.1, round(ScoreModel.spread(1)));
        assertEquals(1.2, round(ScoreModel.spread(3)));
    }

    @Test
    void urgencyWeightsByClass() {
        // KEY_ESTABLISHMENT base 3, confidentiality x2.0 = 6.0
        assertEquals(6.0, ScoreModel.findingUrgency(finding(Category.KEY_ESTABLISHMENT, List.of())));
        // SIGNATURE base 3, signature x1.0 = 3.0
        assertEquals(3.0, ScoreModel.findingUrgency(finding(Category.SIGNATURE, List.of())));
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
