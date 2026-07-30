package org.pqcreadiness.auditor.model;

import java.util.List;

/**
 * Per-file slice of a readiness report: the findings in one source file and their
 * summed difficulty.
 *
 * @param testScoped whether the file sits under a conventional test source root. Test
 *                   code exercises vulnerable algorithms deliberately, so it is reported
 *                   separately from production migration surface.
 */
public record FileReport(String path, double score, boolean testScoped, List<Finding> findings) {

    public FileReport {
        findings = List.copyOf(findings);
    }

    /** Production-scoped file report. */
    public FileReport(String path, double score, List<Finding> findings) {
        this(path, score, false, findings);
    }
}
