package org.pqcreadiness.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.pqcreadiness.auditor.model.ReadinessReport;
import org.pqcreadiness.auditor.report.JsonReportWriter;
import org.pqcreadiness.auditor.report.MarkdownReportWriter;
import org.pqcreadiness.auditor.report.SarifReportWriter;
import org.pqcreadiness.auditor.scan.ScanResult;
import org.pqcreadiness.auditor.scan.Scanner;
import org.pqcreadiness.auditor.score.ModuleResolver;
import org.pqcreadiness.auditor.score.ScoringEngine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Runs the PQC readiness audit over the project's source tree and writes JSON,
 * Markdown, and SARIF readiness reports.
 *
 * <p>The goal is an aggregator: in a multi-module build it runs once, at the
 * execution root, and scans the whole tree — matching how the CLI treats a
 * repository. Build output directories ({@code target} by default) are excluded
 * so generated sources are never audited.
 *
 * <pre>{@code
 * mvn io.github.arpan0995:pqc-readiness-maven-plugin:audit
 * }</pre>
 */
@Mojo(name = "audit", aggregator = true, threadSafe = true)
public class AuditMojo extends AbstractMojo {

    /** Root of the source tree to scan. Defaults to the multi-module root. */
    @Parameter(property = "pqc.readiness.sourceRoot",
            defaultValue = "${maven.multiModuleProjectDirectory}")
    private File sourceRoot;

    /** Directory the three reports are written to. */
    @Parameter(property = "pqc.readiness.out",
            defaultValue = "${project.build.directory}/pqc-readiness")
    private File outputDirectory;

    /** Codebase label used in the reports. */
    @Parameter(property = "pqc.readiness.name", defaultValue = "${project.artifactId}")
    private String name;

    /**
     * Directory names pruned from the scan wherever they appear, so build output
     * and generated sources are not audited.
     */
    @Parameter(property = "pqc.readiness.excludedDirectories")
    private List<String> excludedDirectories = List.of("target");

    /** Skip the audit entirely. */
    @Parameter(property = "pqc.readiness.skip", defaultValue = "false")
    private boolean skip;

    /** Version stamped into the reports; injected from the plugin descriptor. */
    @Parameter(defaultValue = "${plugin.version}", readonly = true)
    private String pluginVersion;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("PQC readiness audit skipped (pqc.readiness.skip=true).");
            return;
        }
        if (sourceRoot == null || !sourceRoot.exists()) {
            throw new MojoFailureException("Source root does not exist: " + sourceRoot);
        }

        Path root = sourceRoot.toPath().toAbsolutePath().normalize();
        ScanResult scan = new Scanner(new LinkedHashSet<>(excludedDirectories)).scan(root);
        ReadinessReport report = new ScoringEngine(pluginVersion == null ? "unknown" : pluginVersion)
                .score(name == null ? String.valueOf(root.getFileName()) : name,
                        scan, new ModuleResolver(root));

        Path out = outputDirectory.toPath();
        try {
            new JsonReportWriter().write(report, out.resolve("readiness-report.json"));
            new MarkdownReportWriter().write(report, out.resolve("readiness-report.md"));
            new SarifReportWriter().write(report, out.resolve("readiness-report.sarif"));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write readiness reports", e);
        }

        getLog().info(String.format("Scanned %d files (%d skipped), %d findings across %d module(s).",
                report.filesScanned(), report.filesSkipped(), report.totalFindings(),
                report.modules().size()));
        getLog().info("Readiness reports written to " + out.toAbsolutePath());
    }
}
