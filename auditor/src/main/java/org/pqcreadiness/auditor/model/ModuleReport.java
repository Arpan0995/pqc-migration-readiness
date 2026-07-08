package org.pqcreadiness.auditor.model;

import java.util.List;

/**
 * Per-module slice of a readiness report and the unit validated against measured
 * migration effort (see {@code docs/research/05-validation-and-benchmark-plan.md}).
 *
 * @param name          module name (build module or top-level package)
 * @param loc           non-blank lines of Java in the module (confounder control)
 * @param score         difficulty score S (model v0)
 * @param urgency       urgency score U, kept orthogonal to difficulty
 * @param baselineCount naive baseline B0: count of vulnerable call sites
 * @param files         per-file breakdown, ranked by descending file score
 */
public record ModuleReport(
        String name,
        int loc,
        double score,
        double urgency,
        int baselineCount,
        List<FileReport> files) {

    public ModuleReport {
        files = List.copyOf(files);
    }
}
