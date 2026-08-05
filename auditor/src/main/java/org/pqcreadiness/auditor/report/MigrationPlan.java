package org.pqcreadiness.auditor.report;

import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.FileReport;
import org.pqcreadiness.auditor.model.Finding;
import org.pqcreadiness.auditor.model.ModuleReport;
import org.pqcreadiness.auditor.model.ReadinessReport;
import org.pqcreadiness.auditor.score.PlanningTimeModel;
import org.pqcreadiness.auditor.score.ScoreModel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A high-level migration plan derived from a {@link ReadinessReport}: the ordered
 * steps a PQC migration of this codebase needs, which modules each step touches,
 * and an engineer-time range per step and in total, split into change work and
 * testing (planning-time model {@code t0}, see {@link PlanningTimeModel}).
 *
 * <p>Derived view only. Nothing here feeds back into scores or tiers; findings in
 * test source roots are excluded (test crypto is usually deliberate, not production
 * migration surface); and every time figure is a stated-assumption planning
 * heuristic, not a validated effort prediction (doc 03 &sect;8.1).
 *
 * @param timeModel        planning-time model version the figures were derived with
 * @param changeHoursLow   change work across all steps, low bound (engineer-hours)
 * @param changeHoursHigh  change work across all steps, high bound
 * @param testingHoursLow  testing budget, low bound ({@code change x fraction})
 * @param testingHoursHigh testing budget, high bound
 * @param setupHoursLow    one-time setup, low bound (flat, only when work exists)
 * @param setupHoursHigh   one-time setup, high bound
 * @param totalHoursLow    setup + change + testing, low bound
 * @param totalHoursHigh   setup + change + testing, high bound
 * @param steps            ordered steps; only steps with relevant findings appear
 * @param assumptions      the declared conversion assumptions, for self-description
 */
public record MigrationPlan(
        String timeModel,
        double changeHoursLow,
        double changeHoursHigh,
        double testingHoursLow,
        double testingHoursHigh,
        double setupHoursLow,
        double setupHoursHigh,
        double totalHoursLow,
        double totalHoursHigh,
        List<Step> steps,
        Assumptions assumptions) {

    public MigrationPlan {
        steps = List.copyOf(steps);
    }

    /**
     * One step of the plan.
     *
     * @param id        stable machine-readable step identifier
     * @param title     short human title
     * @param action    one- or two-sentence description of what to actually do
     * @param sites     number of findings driving this step (0 for setup/testing)
     * @param modules   modules the step touches, in report (score) order
     * @param hoursLow  engineer-hours for this step, low bound
     * @param hoursHigh engineer-hours for this step, high bound
     * @param effortCountedInOtherSteps true when the step's cost is already inside
     *                  the per-site estimates of other steps (fragility-driven
     *                  steps), so its hours are reported as 0 to avoid double
     *                  counting
     */
    public record Step(
            String id,
            String title,
            String action,
            int sites,
            List<String> modules,
            double hoursLow,
            double hoursHigh,
            boolean effortCountedInOtherSteps) {

        public Step {
            modules = List.copyOf(modules);
        }
    }

    /** The declared assumptions of time model t0, embedded so reports self-describe. */
    public record Assumptions(
            double changeHoursPerPointLow,
            double changeHoursPerPointHigh,
            double testingFractionOfChangeLow,
            double testingFractionOfChangeHigh,
            double setupHoursLow,
            double setupHoursHigh,
            String caveat) {

        static Assumptions current() {
            return new Assumptions(
                    PlanningTimeModel.CHANGE_HOURS_PER_POINT_LOW,
                    PlanningTimeModel.CHANGE_HOURS_PER_POINT_HIGH,
                    PlanningTimeModel.TESTING_FRACTION_LOW,
                    PlanningTimeModel.TESTING_FRACTION_HIGH,
                    PlanningTimeModel.SETUP_HOURS_LOW,
                    PlanningTimeModel.SETUP_HOURS_HIGH,
                    "Engineer-time ranges are planning heuristics derived from the "
                            + "difficulty score via declared assumptions (time model t0), "
                            + "not validated effort predictions; Phase 2 validates or "
                            + "replaces them with measured data.");
        }
    }

    /** Build the plan for a report. Test-scoped files and modules are excluded. */
    public static MigrationPlan of(ReadinessReport report) {
        Map<Category, Agg> byCategory = new EnumMap<>(Category.class);
        Agg persistedKeys = new Agg();
        Agg fixedBuffers = new Agg();

        for (ModuleReport module : report.modules()) {
            if (module.testScoped()) {
                continue;
            }
            // Recover the module's spread factor so per-step sums add up to the
            // module scores the ranking shows: ratio = S / sum(d(f)).
            double summed = module.files().stream()
                    .flatMap(f -> f.findings().stream())
                    .mapToDouble(ScoreModel::findingDifficulty)
                    .sum();
            double ratio = summed > 0 ? module.score() / summed : 0.0;
            for (FileReport file : module.files()) {
                if (file.testScoped()) {
                    continue;
                }
                for (Finding finding : file.findings()) {
                    double difficulty = ScoreModel.findingDifficulty(finding);
                    if (difficulty <= 0) {
                        continue;
                    }
                    byCategory.computeIfAbsent(finding.category(), c -> new Agg())
                            .add(module.name(), difficulty * ratio);
                    if (finding.fragility().contains("F6")) {
                        persistedKeys.add(module.name(), 0.0);
                    }
                    if (finding.fragility().contains("F1")) {
                        fixedBuffers.add(module.name(), 0.0);
                    }
                }
            }
        }

        double points = byCategory.values().stream().mapToDouble(a -> a.effort).sum();
        // Round each part once, up front, so steps, the split and the total all
        // visibly add up to the same figures.
        double changeLow = round(points * PlanningTimeModel.CHANGE_HOURS_PER_POINT_LOW);
        double changeHigh = round(points * PlanningTimeModel.CHANGE_HOURS_PER_POINT_HIGH);
        double testLow = round(changeLow * PlanningTimeModel.TESTING_FRACTION_LOW);
        double testHigh = round(changeHigh * PlanningTimeModel.TESTING_FRACTION_HIGH);
        boolean anyWork = points > 0;
        double setupLow = anyWork ? PlanningTimeModel.SETUP_HOURS_LOW : 0.0;
        double setupHigh = anyWork ? PlanningTimeModel.SETUP_HOURS_HIGH : 0.0;

        List<Step> steps = new ArrayList<>();
        if (anyWork) {
            steps.add(new Step("setup",
                    "Set up a PQC provider and an agility seam",
                    "Add a provider that ships the FIPS 203/204/205 algorithms "
                            + "(Bouncy Castle today; JDK 24+ ships ML-KEM/ML-DSA natively) "
                            + "and route algorithm selection through one seam so every "
                            + "later step swaps algorithms in one place.",
                    0, List.of(), setupLow, setupHigh, false));
        }
        addCategoryStep(steps, byCategory, Category.TYPE_COUPLING, "decouple-key-types",
                "Decouple concrete key types from APIs",
                "Replace RSAPublicKey, ECPrivateKey and other concrete key interfaces "
                        + "in method signatures, fields and casts with PublicKey/PrivateKey "
                        + "or an opaque handle so a PQC key can flow through. In mature "
                        + "Java code this API churn, not call-site swaps, is usually the "
                        + "bulk of the work.");
        addCategoryStep(steps, byCategory, Category.KEYGEN, "keygen",
                "Regenerate keys with PQC or hybrid algorithms",
                "Move KeyPairGenerator/KeyFactory/parameter setup from RSA/EC to ML-KEM "
                        + "or ML-DSA (hybrid where the transition needs both), and budget "
                        + "for much larger keys: PQC public keys run 1.2-2.6 KB.");
        addCategoryStep(steps, byCategory, Category.KEY_ESTABLISHMENT, "key-establishment",
                "Migrate key establishment to ML-KEM (hybrid first)",
                "Replace RSA encryption and (EC)DH agreement with ML-KEM-768, preferably "
                        + "as the X25519MLKEM768 hybrid. Confidentiality is exposed to "
                        + "harvest-now-decrypt-later today, so schedule these sites first.");
        addCategoryStep(steps, byCategory, Category.SIGNATURE, "signatures",
                "Migrate signatures to ML-DSA",
                "Swap RSA/ECDSA signing and verification to ML-DSA-65, or run a dual "
                        + "classical+PQC signature during the transition. Expect 3.3 KB "
                        + "signatures and slower signing; recheck every size assumption "
                        + "downstream of a signature.");
        addCategoryStep(steps, byCategory, Category.JOSE, "jose",
                "Re-pin JOSE/JWT algorithms and token sizes",
                "Move pinned token algorithms to PQC signatures and re-check token-size "
                        + "budgets: PQC-signed JWTs outgrow cookie and header limits that "
                        + "classical tokens fit comfortably.");
        addCategoryStep(steps, byCategory, Category.TLS_CONFIG, "tls-config",
                "Rework pinned TLS configuration",
                "Remove legacy protocol and cipher-suite pins so hybrid key exchange "
                        + "(X25519MLKEM768) can be negotiated where the runtime supports it.");
        if (persistedKeys.sites > 0) {
            steps.add(new Step("persisted-keys",
                    "Re-issue persisted key material",
                    "Key stores and encoded keys near flagged sites must be regenerated, "
                            + "re-encoded and redistributed; plan the data migration, a "
                            + "dual-trust window and a rollback path.",
                    persistedKeys.sites, List.copyOf(persistedKeys.modules),
                    0.0, 0.0, true));
        }
        if (fixedBuffers.sites > 0) {
            steps.add(new Step("buffers",
                    "Widen fixed-size buffers and wire formats",
                    "Buffers sized for classical artifacts cannot hold multi-KB PQC keys, "
                            + "ciphertexts or signatures; widen them together with the "
                            + "call-site changes they sit next to.",
                    fixedBuffers.sites, List.copyOf(fixedBuffers.modules),
                    0.0, 0.0, true));
        }
        if (anyWork) {
            steps.add(new Step("test-and-rollout",
                    "Test and roll out",
                    "Add round-trip and tamper tests for each migrated primitive, interop "
                            + "tests against the pre-migration stack, and a performance "
                            + "pass: PQC allocates 4-13x more memory per operation, so "
                            + "watch GC under sustained load. Roll out behind a fallback "
                            + "path.",
                    0, List.of(), testLow, testHigh, false));
        }

        return new MigrationPlan(PlanningTimeModel.VERSION,
                changeLow, changeHigh,
                testLow, testHigh,
                setupLow, setupHigh,
                round(setupLow + changeLow + testLow),
                round(setupHigh + changeHigh + testHigh),
                steps, Assumptions.current());
    }

    /** True when the scan found no scored production crypto, so there is no plan. */
    public boolean isEmpty() {
        return totalHoursHigh <= 0;
    }

    /**
     * Render an hour range in the most readable unit for its size, e.g.
     * {@code ~3-6 hours}, {@code ~2-4 days}, {@code ~4-9 weeks}, {@code ~4-8 months}.
     */
    public static String humanRange(double lowHours, double highHours) {
        if (highHours <= 0) {
            return "none";
        }
        String unit;
        double divisor;
        if (highHours <= 16) {
            unit = "hour";
            divisor = 1.0;
        } else if (highHours <= 15 * PlanningTimeModel.HOURS_PER_DAY) {
            unit = "day";
            divisor = PlanningTimeModel.HOURS_PER_DAY;
        } else if (highHours <= 26 * PlanningTimeModel.HOURS_PER_WEEK) {
            unit = "week";
            divisor = PlanningTimeModel.HOURS_PER_WEEK;
        } else {
            unit = "month";
            divisor = PlanningTimeModel.HOURS_PER_MONTH;
        }
        long low = Math.max(1, Math.round(lowHours / divisor));
        long high = Math.max(low, Math.round(highHours / divisor));
        String range = low == high ? "~" + low : "~" + low + "–" + high;
        return range + " " + unit + (high == 1 ? "" : "s");
    }

    private static void addCategoryStep(List<Step> steps, Map<Category, Agg> byCategory,
                                        Category category, String id, String title,
                                        String action) {
        Agg agg = byCategory.get(category);
        if (agg == null || agg.sites == 0) {
            return;
        }
        steps.add(new Step(id, title, action, agg.sites, List.copyOf(agg.modules),
                round(agg.effort * PlanningTimeModel.CHANGE_HOURS_PER_POINT_LOW),
                round(agg.effort * PlanningTimeModel.CHANGE_HOURS_PER_POINT_HIGH),
                false));
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /** Mutable accumulator: finding count, summed effective difficulty, module names. */
    private static final class Agg {
        int sites;
        double effort;
        final Set<String> modules = new LinkedHashSet<>();

        void add(String module, double effectiveDifficulty) {
            sites++;
            effort += effectiveDifficulty;
            modules.add(module);
        }
    }
}
