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

        appendMigrationPlan(md, report);

        List<ModuleReport> production = report.modules().stream()
                .filter(m -> !m.testScoped()).toList();
        List<ModuleReport> tests = report.modules().stream()
                .filter(ModuleReport::testScoped).toList();

        appendModuleRanking(md, "Module ranking", production);
        if (!tests.isEmpty()) {
            md.append("Test-only modules are ranked separately below. Their crypto is "
                    + "usually deliberate test material rather than production migration "
                    + "surface, so mixing them into the ranking overstates the work.\n\n");
            appendModuleRanking(md, "Test-only modules", tests);
        }
        for (ModuleReport module : production) {
            appendModuleDetail(md, module);
        }
        for (ModuleReport module : tests) {
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

    private void appendMigrationPlan(StringBuilder md, ReadinessReport report) {
        MigrationPlan plan = MigrationPlan.of(report);
        md.append("## Migration plan at a glance\n\n");
        if (plan.isEmpty()) {
            md.append("No quantum-vulnerable production crypto was found, so there is "
                    + "nothing to plan: this codebase needs no PQC migration work on the "
                    + "surface this auditor sees.\n\n");
            return;
        }

        md.append("**Estimated effort for one engineer: ")
                .append(MigrationPlan.humanRange(plan.totalHoursLow(), plan.totalHoursHigh()))
                .append("** — change work ")
                .append(MigrationPlan.humanRange(plan.changeHoursLow(), plan.changeHoursHigh()))
                .append(", testing ")
                .append(MigrationPlan.humanRange(plan.testingHoursLow(), plan.testingHoursHigh()))
                .append(", plus ")
                .append(MigrationPlan.humanRange(plan.setupHoursLow(), plan.setupHoursHigh()))
                .append(" of one-time setup.\n\n");

        md.append("> Time figures come from planning-time model `").append(plan.timeModel())
                .append("`, layered on the difficulty score: 1 score point ≈ ")
                .append(plan.assumptions().changeHoursPerPointLow()).append('–')
                .append(plan.assumptions().changeHoursPerPointHigh())
                .append(" engineer-hours of change work; testing ≈ 50–100% of change. ")
                .append("These are declared planning assumptions, not validated "
                        + "predictions (doc 03 §8.1). Findings under test source roots "
                        + "are excluded from the plan.\n\n");

        md.append("| # | Step | Sites | Where | Effort |\n");
        md.append("|---:|---|---:|---|---|\n");
        int number = 1;
        for (MigrationPlan.Step step : plan.steps()) {
            md.append("| ").append(number++).append(" | ").append(step.title())
                    .append(" | ").append(step.sites() > 0 ? step.sites() : "—")
                    .append(" | ").append(moduleList(step.modules()))
                    .append(" | ")
                    .append(step.effortCountedInOtherSteps()
                            ? "_counted in the steps above_"
                            : MigrationPlan.humanRange(step.hoursLow(), step.hoursHigh()))
                    .append(" |\n");
        }
        md.append("\n");

        number = 1;
        for (MigrationPlan.Step step : plan.steps()) {
            md.append(number++).append(". **").append(step.title()).append(".** ")
                    .append(step.action()).append('\n');
        }
        md.append("\n");
    }

    private static String moduleList(List<String> modules) {
        if (modules.isEmpty()) {
            return "—";
        }
        StringBuilder cell = new StringBuilder();
        int shown = Math.min(3, modules.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                cell.append(", ");
            }
            cell.append('`').append(modules.get(i)).append('`');
        }
        if (modules.size() > shown) {
            cell.append(" +").append(modules.size() - shown).append(" more");
        }
        return cell.toString();
    }

    private void appendModuleRanking(StringBuilder md, String heading, List<ModuleReport> modules) {
        md.append("## ").append(heading).append("\n\n");
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
