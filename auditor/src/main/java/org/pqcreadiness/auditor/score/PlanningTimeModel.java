package org.pqcreadiness.auditor.score;

/**
 * Planning-time model, version {@code t0}: declared assumptions for converting
 * difficulty-score points (score model v0) into engineer-time ranges for the
 * report's migration plan.
 *
 * <p>This is a <em>separate, separately versioned layer</em> on top of the frozen
 * score model — {@link ScoreModel} v0 and its pre-registration (doc 03) are not
 * touched by anything here, and nothing here feeds back into scores or tiers.
 * The constants are stated planning assumptions, not measured conversion factors:
 * every range is deliberately wide, every derived figure is labelled a heuristic
 * in the reports, and Phase 2 (doc 05) validates or replaces this layer with
 * measured effort data. See doc 03 &sect;8.1 for why the layer exists at all.
 *
 * <p>Anchors for the change-work range: one difficulty point is one base-weight
 * unit, so a bare concrete-key-type coupling site is 1 point and an unfragile
 * signature call site is 3. At 1.5&ndash;3.0 engineer-hours per point, the
 * coupling site prices at roughly 1.5&ndash;3 hours (widen the type, fix casts
 * and callers, review) and the signature call site at half a day to a day (swap
 * the algorithm, adapt size assumptions, review). Fragility multipliers and the
 * spread factor scale time exactly as they scale the score. Testing is budgeted
 * as a fraction of change work — round-trip and tamper tests, dual-stack
 * interop, and a performance regression pass.
 */
public final class PlanningTimeModel {

    public static final String VERSION = "t0";

    /** Engineer-hours of change work per difficulty-score point, low bound. */
    public static final double CHANGE_HOURS_PER_POINT_LOW = 1.5;
    /** Engineer-hours of change work per difficulty-score point, high bound. */
    public static final double CHANGE_HOURS_PER_POINT_HIGH = 3.0;

    /** Testing effort as a fraction of change effort, low bound. */
    public static final double TESTING_FRACTION_LOW = 0.5;
    /** Testing effort as a fraction of change effort, high bound. */
    public static final double TESTING_FRACTION_HIGH = 1.0;

    /** One-time setup (provider wiring, agility seam, test rig), low bound in hours. */
    public static final double SETUP_HOURS_LOW = 24.0;
    /** One-time setup (provider wiring, agility seam, test rig), high bound in hours. */
    public static final double SETUP_HOURS_HIGH = 40.0;

    /** Working hours per engineer-day, for display conversion. */
    public static final double HOURS_PER_DAY = 8.0;
    /** Working hours per engineer-week, for display conversion. */
    public static final double HOURS_PER_WEEK = 40.0;
    /** Working hours per engineer-month (~4.33 weeks), for display conversion. */
    public static final double HOURS_PER_MONTH = 173.0;

    private PlanningTimeModel() {
    }
}
