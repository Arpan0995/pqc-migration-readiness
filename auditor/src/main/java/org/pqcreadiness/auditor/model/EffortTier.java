package org.pqcreadiness.auditor.model;

/**
 * A qualitative migration-effort estimate derived from a module's difficulty score.
 *
 * <p><strong>This is a heuristic, not a validated prediction.</strong> The project's
 * research plan (see {@code docs/research/03-difficulty-scoring-model.md} &sect;8) has
 * two phases: Phase 1 (current) produces this score-derived estimate for real public
 * codebases; Phase 2 (deferred) validates the score against measured migration effort.
 * Until Phase 2 runs, treat these tiers as an informed ranking aid, not a measured
 * effort figure — there is deliberately no "N engineer-days" number here, because we
 * have no evidence yet for what conversion factor would be honest.
 *
 * <p>Thresholds are picked from the scoring model's own arithmetic (base weights 1-3,
 * fragility multipliers 1.5-2.5x each, capped at 6x product, spread factor ~1.0-1.4 for
 * realistic file counts) so that each tier corresponds to a roughly-sized cluster of
 * findings: a few simple sites, a moderate set with some fragility stacking, many sites
 * or heavy fragility stacking, and a large or highly fragile surface.
 */
public enum EffortTier {
    /** No scored (vulnerable) findings in this module. */
    NONE(0.0),
    /** A handful of simple findings, little or no fragility stacking. */
    LOW(0.01),
    /** A moderate number of findings and/or some fragility stacking. */
    MEDIUM(10.0),
    /** Many findings and/or significant fragility stacking. */
    HIGH(40.0),
    /** A large or heavily fragile crypto surface. */
    CRITICAL(120.0);

    private final double minScore;

    EffortTier(double minScore) {
        this.minScore = minScore;
    }

    /** Classify a module's difficulty score into a tier using the thresholds above. */
    public static EffortTier forScore(double score) {
        EffortTier tier = NONE;
        for (EffortTier candidate : values()) {
            if (score >= candidate.minScore) {
                tier = candidate;
            }
        }
        return tier;
    }
}
