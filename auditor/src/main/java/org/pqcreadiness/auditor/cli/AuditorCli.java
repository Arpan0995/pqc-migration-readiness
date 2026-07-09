package org.pqcreadiness.auditor.cli;

import org.pqcreadiness.auditor.model.ReadinessReport;
import org.pqcreadiness.auditor.report.JsonReportWriter;
import org.pqcreadiness.auditor.report.MarkdownReportWriter;
import org.pqcreadiness.auditor.scan.ScanResult;
import org.pqcreadiness.auditor.scan.Scanner;
import org.pqcreadiness.auditor.score.ModuleResolver;
import org.pqcreadiness.auditor.score.ScoringEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Command-line entry point: scans a Java codebase and writes a readiness report as
 * JSON and Markdown.
 *
 * <pre>{@code
 * auditor <source-root> [--out <dir>] [--name <codebase-label>]
 * }</pre>
 *
 * Defaults: {@code --out ./audit-out}, {@code --name} = the source root's file name.
 */
public final class AuditorCli {

    private static final String VERSION = "1.0.0";

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || isHelp(args[0])) {
            printUsage();
            System.exit(args.length == 0 ? 2 : 0);
            return;
        }

        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Path out = Path.of("audit-out");
        String name = root.getFileName() == null ? "codebase" : root.getFileName().toString();

        for (int i = 1; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--out" -> out = Path.of(args[++i]);
                case "--name" -> name = args[++i];
                default -> { }
            }
        }

        if (!Files.exists(root)) {
            System.err.println("Source root does not exist: " + root);
            System.exit(2);
            return;
        }

        ScanResult scan = new Scanner().scan(root);
        ReadinessReport report = new ScoringEngine(VERSION)
                .score(name, scan, new ModuleResolver(root));

        Path jsonOut = out.resolve("readiness-report.json");
        Path mdOut = out.resolve("readiness-report.md");
        new JsonReportWriter().write(report, jsonOut);
        new MarkdownReportWriter().write(report, mdOut);

        System.out.printf("Scanned %d files (%d skipped), %d findings across %d module(s).%n",
                report.filesScanned(), report.filesSkipped(), report.totalFindings(),
                report.modules().size());
        System.out.println("JSON report:     " + jsonOut.toAbsolutePath());
        System.out.println("Markdown report: " + mdOut.toAbsolutePath());
    }

    private static boolean isHelp(String arg) {
        return arg.equals("-h") || arg.equals("--help");
    }

    private static void printUsage() {
        System.out.println("""
                Usage: auditor <source-root> [--out <dir>] [--name <label>]

                Scans a Java codebase for quantum-vulnerable cryptographic usage and
                writes a PQC migration readiness report (JSON + Markdown).

                  <source-root>   directory (or single .java file) to scan
                  --out <dir>     output directory (default: ./audit-out)
                  --name <label>  codebase label in the report (default: source-root name)
                """);
    }

    private AuditorCli() {
    }
}
