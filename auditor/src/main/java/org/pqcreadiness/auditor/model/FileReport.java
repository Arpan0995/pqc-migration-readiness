package org.pqcreadiness.auditor.model;

import java.util.List;

/**
 * Per-file slice of a readiness report: the findings in one source file and their
 * summed difficulty.
 */
public record FileReport(String path, double score, List<Finding> findings) {

    public FileReport {
        findings = List.copyOf(findings);
    }
}
