package org.pqcreadiness.auditor.report;

import org.junit.jupiter.api.Test;
import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.Confidence;
import org.pqcreadiness.auditor.model.EffortTier;
import org.pqcreadiness.auditor.model.FileReport;
import org.pqcreadiness.auditor.model.Finding;
import org.pqcreadiness.auditor.model.ModuleReport;
import org.pqcreadiness.auditor.model.ReadinessReport;
import org.pqcreadiness.auditor.score.ScoreModel;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the derived migration plan and planning-time model t0 arithmetic. */
class MigrationPlanTest {

    private static Finding finding(String file, Category category, List<String> fragility) {
        return new Finding("RULE", file, 1, 1, "api", "RSA", category,
                Confidence.HIGH, fragility, "snippet");
    }

    /** Build a one-module report whose score matches the engine's arithmetic. */
    private static ReadinessReport report(boolean testScoped, Finding... findings) {
        double summed = 0.0;
        for (Finding f : findings) {
            summed += ScoreModel.findingDifficulty(f);
        }
        double score = summed * ScoreModel.spread(1);
        FileReport file = new FileReport("A.java", summed, testScoped, List.of(findings));
        ModuleReport module = new ModuleReport("m", 100, score, EffortTier.forScore(score),
                0.0, findings.length, testScoped, List.of(file));
        return new ReadinessReport("cb", "test", "v0", "now", findings.length, 1, 0,
                List.of(module));
    }

    @Test
    void emptyReportHasNoPlan() {
        ReadinessReport empty = new ReadinessReport("cb", "test", "v0", "now", 0, 0, 0, List.of());
        MigrationPlan plan = MigrationPlan.of(empty);
        assertTrue(plan.isEmpty());
        assertTrue(plan.steps().isEmpty());
        assertEquals(0.0, plan.totalHoursHigh());
    }

    @Test
    void signatureFindingProducesSetupSignatureAndTestSteps() {
        MigrationPlan plan = MigrationPlan.of(
                report(false, finding("A.java", Category.SIGNATURE, List.of())));

        List<String> ids = plan.steps().stream().map(MigrationPlan.Step::id).toList();
        assertEquals(List.of("setup", "signatures", "test-and-rollout"), ids);

        // 3 points x spread(1 file) = 3.3; change = 3.3 x [1.5, 3.0] = [4.95, 9.9] -> rounded.
        double effective = 3.0 * ScoreModel.spread(1);
        assertEquals(round1(effective * 1.5), plan.changeHoursLow());
        assertEquals(round1(effective * 3.0), plan.changeHoursHigh());
        // Testing is 50-100% of change; totals add setup 24-40h on top.
        assertEquals(round1(plan.changeHoursLow() * 0.5), plan.testingHoursLow());
        assertEquals(round1(plan.changeHoursHigh() * 1.0), plan.testingHoursHigh());
        assertEquals(24.0, plan.setupHoursLow());
        assertEquals(40.0, plan.setupHoursHigh());
        assertEquals(
                round1(plan.setupHoursLow() + plan.changeHoursLow() + plan.testingHoursLow()),
                plan.totalHoursLow());
    }

    @Test
    void categoryStepsCarryTheirSitesAndModules() {
        MigrationPlan plan = MigrationPlan.of(report(false,
                finding("A.java", Category.TYPE_COUPLING, List.of("F4")),
                finding("A.java", Category.KEY_ESTABLISHMENT, List.of())));

        MigrationPlan.Step decouple = step(plan, "decouple-key-types").orElseThrow();
        assertEquals(1, decouple.sites());
        assertEquals(List.of("m"), decouple.modules());
        assertTrue(step(plan, "key-establishment").isPresent());
        assertTrue(step(plan, "signatures").isEmpty());
    }

    @Test
    void fragilityStepsAppearButCountNoExtraHours() {
        MigrationPlan plan = MigrationPlan.of(report(false,
                finding("A.java", Category.SIGNATURE, List.of("F6", "F1"))));

        MigrationPlan.Step keys = step(plan, "persisted-keys").orElseThrow();
        MigrationPlan.Step buffers = step(plan, "buffers").orElseThrow();
        assertTrue(keys.effortCountedInOtherSteps());
        assertEquals(0.0, keys.hoursHigh());
        assertEquals(1, buffers.sites());

        // Total change hours equal the fragility-multiplied signature site alone:
        // 3 x (2.0 x 1.5) x spread(1) points, no double counting from the two steps.
        double effective = 9.0 * ScoreModel.spread(1);
        assertEquals(round1(effective * 3.0), plan.changeHoursHigh());
    }

    @Test
    void testScopedFindingsAreExcluded() {
        MigrationPlan plan = MigrationPlan.of(
                report(true, finding("src/test/java/A.java", Category.SIGNATURE, List.of())));
        assertTrue(plan.isEmpty());
        assertTrue(plan.steps().isEmpty());
    }

    @Test
    void informationalFindingsProduceNoPlan() {
        MigrationPlan plan = MigrationPlan.of(
                report(false, finding("A.java", Category.INFORMATIONAL, List.of())));
        assertTrue(plan.isEmpty());
    }

    @Test
    void humanRangePicksReadableUnits() {
        assertEquals("none", MigrationPlan.humanRange(0, 0));
        assertEquals("~3–6 hours", MigrationPlan.humanRange(3, 6));
        assertEquals("~1 hour", MigrationPlan.humanRange(0.6, 1.2));
        assertEquals("~3–5 days", MigrationPlan.humanRange(24, 40));
        assertEquals("~2–4 weeks", MigrationPlan.humanRange(80, 160));
        assertEquals("~7–15 months", MigrationPlan.humanRange(1200, 2600));
    }

    private static Optional<MigrationPlan.Step> step(MigrationPlan plan, String id) {
        return plan.steps().stream().filter(s -> s.id().equals(id)).findFirst();
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
