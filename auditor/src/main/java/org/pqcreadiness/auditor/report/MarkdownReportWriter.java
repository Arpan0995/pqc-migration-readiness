package org.pqcreadiness.auditor.report;

import org.pqcreadiness.auditor.model.FileReport;
import org.pqcreadiness.auditor.model.Finding;
import org.pqcreadiness.auditor.model.ModuleReport;
import org.pqcreadiness.auditor.model.ReadinessReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Renders a {@link ReadinessReport} as human-readable Markdown: a module ranking by
 * difficulty score, then per-module hotspots with {@code file:line} references and a
 * plain-language reason each site is expensive to migrate.
 */
public final class MarkdownReportWriter {

    /** Cap on hotspot findings listed per module, to keep large reports readable. */
    private static final int MAX_HOTSPOTS_PER_MODULE = 15;

    public String toMarkdown(ReadinessReport report) {
        StringBuilder md = new StringBuilder();
        md.append("# PQC Migration Readiness Report: ").append(report.codebase()).append("\n\n");
        md.append("- Auditor version: `").append(report.auditorVersion()).append("`\n");
        md.append("- Score model: `").append(report.scoreModel()).append("`\n");
        md.append("- Generated: ").append(report.generatedAt()).append("\n");
        md.append("- Findings: ").append(report.totalFindings())
                .append(" across ").append(report.filesScanned()).append(" files");
        if (report.filesSkipped() > 0) {
            md.append(" (").append(report.filesSkipped()).append(" skipped: parse errors)");
        }
        md.append("\n\n");

        md.append("> Difficulty score **S** and effort **tier** are a heuristic estimate, "
                + "not yet a validated prediction of migration effort (see "
                + "`docs/research/03-difficulty-scoring-model.md` §8 for the estimation-vs-"
                + "validation phasing). The naive baseline **B0** is a raw count of vulnerable "
                + "call sites. Urgency **U** is a separate axis (harvest-now-decrypt-later risk), "
                + "not part of the difficulty estimate.\n\n");

        appendModuleRanking(md, report.modules());
        for (ModuleReport module : report.modules()) {
            appendModuleDetail(md, module);
        }
        return md.toString();
    }

    public void write(ReadinessReport report, Path out) throws IOException {
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Files.writeString(out, toMarkdown(report));
    }

    private void appendModuleRanking(StringBuilder md, List<ModuleReport> modules) {
        md.append("## Module ranking\n\n");
        md.append("| Rank | Module | Tier | Score S | Urgency U | Baseline B0 | LOC |\n");
        md.append("|---:|---|---|---:|---:|---:|---:|\n");
        int rank = 1;
        for (ModuleReport m : modules) {
            md.append("| ").append(rank++).append(" | `").append(m.name()).append("` | ")
                    .append(m.tier()).append(" | ")
                    .append(m.score()).append(" | ").append(m.urgency()).append(" | ")
                    .append(m.baselineCount()).append(" | ").append(m.loc()).append(" |\n");
        }
        md.append("\n");
    }

    private void appendModuleDetail(StringBuilder md, ModuleReport module) {
        md.append("## Module: `").append(module.name()).append("`\n\n");
        md.append("Effort tier **").append(module.tier()).append("** (score ")
                .append(module.score()).append("), urgency ")
                .append(module.urgency()).append(", ").append(module.baselineCount())
                .append(" vulnerable call sites, ").append(module.loc()).append(" LOC.\n\n");

        int shown = 0;
        boolean any = false;
        for (FileReport file : module.files()) {
            for (Finding finding : file.findings()) {
                if (shown >= MAX_HOTSPOTS_PER_MODULE) {
                    md.append("\n_").append(remaining(module, shown))
                            .append(" more finding(s) not shown._\n");
                    md.append("\n");
                    return;
                }
                if (!any) {
                    md.append("| Site | Rule | Difficulty | Why it is expensive |\n");
                    md.append("|---|---|---:|---|\n");
                    any = true;
                }
                md.append("| `").append(finding.file()).append(':').append(finding.line())
                        .append("` | `").append(finding.ruleId()).append("` | ")
                        .append(round(org.pqcreadiness.auditor.score.ScoreModel.findingDifficulty(finding)))
                        .append(" | ").append(Explanations.why(finding)).append(" |\n");
                shown++;
            }
        }
        md.append("\n");
    }

    private int remaining(ModuleReport module, int shown) {
        int total = module.files().stream().mapToInt(f -> f.findings().size()).sum();
        return total - shown;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
