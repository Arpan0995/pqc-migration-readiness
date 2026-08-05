package org.pqcreadiness.auditor.cli;

import org.pqcreadiness.auditor.model.ReadinessReport;
import org.pqcreadiness.auditor.report.JsonReportWriter;
import org.pqcreadiness.auditor.report.MarkdownReportWriter;
import org.pqcreadiness.auditor.report.MigrationPlan;
import org.pqcreadiness.auditor.report.SarifReportWriter;
import org.pqcreadiness.auditor.scan.ScanResult;
import org.pqcreadiness.auditor.scan.Scanner;
import org.pqcreadiness.auditor.score.ModuleResolver;
import org.pqcreadiness.auditor.score.ScoringEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Command-line entry point: scans a Java codebase and writes a readiness report as
 * JSON, Markdown, and SARIF (for GitHub code scanning).
 *
 * <pre>{@code
 * auditor <source-root> [--out <dir>] [--name <codebase-label>]
 * }</pre>
 *
 * Defaults: {@code --out ./audit-out}, {@code --name} = the source root's file name.
 */
public final class AuditorCli {

    private static final String VERSION = "1.4.0";

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || isHelp(args[0])) {
            printUsage();
            System.exit(args.length == 0 ? 2 : 0);
            return;
        }

        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Path out = Path.of("audit-out");
        String name = root.getFileName() == null ? "codebase" : root.getFileName().toString();
        Set<String> excluded = new LinkedHashSet<>(DEFAULT_EXCLUDES);
        boolean skipTests = false;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--out" -> out = Path.of(requireValue(args, ++i, "--out"));
                case "--name" -> name = requireValue(args, ++i, "--name");
                case "--exclude" -> excluded = parseExcludes(requireValue(args, ++i, "--exclude"));
                case "--skip-tests" -> skipTests = true;
                default -> {
                    // Silently ignoring a mistyped option would corrupt a measurement run.
                    System.err.println("Unknown option: " + args[i]);
                    printUsage();
                    System.exit(2);
                    return;
                }
            }
        }

        if (!Files.exists(root)) {
            System.err.println("Source root does not exist: " + root);
            System.exit(2);
            return;
        }

        ScanResult scan = new Scanner(excluded, skipTests).scan(root);
        ReadinessReport report = new ScoringEngine(VERSION)
                .score(name, scan, new ModuleResolver(root));

        Path jsonOut = out.resolve("readiness-report.json");
        Path mdOut = out.resolve("readiness-report.md");
        Path sarifOut = out.resolve("readiness-report.sarif");
        new JsonReportWriter().write(report, jsonOut);
        new MarkdownReportWriter().write(report, mdOut);
        new SarifReportWriter().write(report, sarifOut);

        System.out.printf("Scanned %d files (%d skipped), %d findings across %d module(s).%n",
                report.filesScanned(), report.filesSkipped(), report.totalFindings(),
                report.modules().size());
        MigrationPlan plan = MigrationPlan.of(report);
        if (plan.isEmpty()) {
            System.out.println("No quantum-vulnerable production findings; "
                    + "no migration effort to estimate.");
        } else {
            System.out.printf("Estimated migration effort (planning heuristic, time model %s): "
                            + "%s for one engineer — change %s, testing %s, one-time setup %s.%n",
                    plan.timeModel(),
                    MigrationPlan.humanRange(plan.totalHoursLow(), plan.totalHoursHigh()),
                    MigrationPlan.humanRange(plan.changeHoursLow(), plan.changeHoursHigh()),
                    MigrationPlan.humanRange(plan.testingHoursLow(), plan.testingHoursHigh()),
                    MigrationPlan.humanRange(plan.setupHoursLow(), plan.setupHoursHigh()));
        }
        System.out.println("JSON report:     " + jsonOut.toAbsolutePath());
        System.out.println("Markdown report: " + mdOut.toAbsolutePath());
        System.out.println("SARIF report:    " + sarifOut.toAbsolutePath());
    }

    /** Build-output directory names skipped unless {@code --exclude} overrides them. */
    private static final List<String> DEFAULT_EXCLUDES = List.of("target", "build", "out", "bin");

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            System.err.println("Option " + option + " requires a value.");
            printUsage();
            System.exit(2);
        }
        return args[index];
    }

    /** Comma-separated directory names; an empty value disables exclusion entirely. */
    private static Set<String> parseExcludes(String value) {
        Set<String> names = new LinkedHashSet<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }
        return names;
    }

    private static boolean isHelp(String arg) {
        return arg.equals("-h") || arg.equals("--help");
    }

    private static void printUsage() {
        System.out.println("""
                Usage: auditor <source-root> [--out <dir>] [--name <label>]
                               [--exclude <dirs>] [--skip-tests]

                Scans a Java codebase for quantum-vulnerable cryptographic usage and
                writes a PQC migration readiness report (JSON + Markdown + SARIF).

                  <source-root>     directory (or single .java file) to scan
                  --out <dir>       output directory (default: ./audit-out)
                  --name <label>    codebase label in the report (default: source-root name)
                  --exclude <dirs>  comma-separated directory names pruned anywhere under
                                    the root (default: target,build,out,bin). Pass an empty
                                    value to scan everything, including build output.
                  --skip-tests      omit conventional test source roots (src/test, and the
                                    testFixtures, integrationTest, androidTest and it
                                    variants). Off by default: test findings are reported
                                    separately, so totals stay comparable between runs.
                """);
    }

    private AuditorCli() {
    }
}
