package org.pqcreadiness.auditor.model;

import java.util.List;

/**
 * Top-level readiness report for one scanned codebase — the primary auditor artifact.
 * Serialised to JSON (machine-readable, consumed by the step-4 analysis harness) and
 * rendered to Markdown (human-readable hotspot ranking).
 *
 * @param codebase       label for the scanned codebase
 * @param auditorVersion auditor build version
 * @param scoreModel     scoring model version (e.g. {@code v0})
 * @param generatedAt    ISO-8601 timestamp of the scan
 * @param totalFindings  total findings across all modules
 * @param filesScanned   number of source files parsed
 * @param filesSkipped   number of files that failed to parse
 * @param skippedFiles   skipped files with their root-relative paths and reasons
 * @param modules        module reports, ranked by descending score
 */
public record ReadinessReport(
        String codebase,
        String auditorVersion,
        String scoreModel,
        String generatedAt,
        int totalFindings,
        int filesScanned,
        int filesSkipped,
        List<SkippedFile> skippedFiles,
        List<ModuleReport> modules) {

    public ReadinessReport {
        skippedFiles = List.copyOf(skippedFiles);
        modules = List.copyOf(modules);
    }

    /** Keeps the previous constructor available for existing report callers. */
    public ReadinessReport(
            String codebase,
            String auditorVersion,
            String scoreModel,
            String generatedAt,
            int totalFindings,
            int filesScanned,
            int filesSkipped,
            List<ModuleReport> modules) {
        this(codebase, auditorVersion, scoreModel, generatedAt, totalFindings,
                filesScanned, filesSkipped, List.of(), modules);
    }
}