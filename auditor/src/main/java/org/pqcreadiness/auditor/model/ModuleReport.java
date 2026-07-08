package org.pqcreadiness.auditor.model;

import java.util.List;

/**
 * Per-module slice of a readiness report. In the project's current phase (estimation
 * only, see {@code docs/research/03-difficulty-scoring-model.md} &sect;8) this is a
 * score-derived effort estimate; it becomes the unit validated against measured
 * migration effort once Phase 2 (see
 * {@code docs/research/05-validation-and-benchmark-plan.md}) runs.
 *
 * @param name          module name (build module or top-level package)
 * @param loc           non-blank lines of Java in the module (confounder control)
 * @param score         difficulty score S (model v0)
 * @param tier          qualitative effort tier derived from {@code score}; a heuristic
 *                      estimate, not yet a validated prediction (see {@link EffortTier})
 * @param urgency       urgency score U, kept orthogonal to difficulty
 * @param baselineCount naive baseline B0: count of vulnerable call sites
 * @param files         per-file breakdown, ranked by descending file score
 */
public record ModuleReport(
        String name,
        int loc,
        double score,
        EffortTier tier,
        double urgency,
        int baselineCount,
        List<FileReport> files) {

    public ModuleReport {
        files = List.copyOf(files);
    }
}
