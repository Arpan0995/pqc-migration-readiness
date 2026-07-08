package org.pqcreadiness.auditor.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pqcreadiness.auditor.model.ReadinessReport;
import org.pqcreadiness.auditor.scan.ScanResult;
import org.pqcreadiness.auditor.scan.Scanner;
import org.pqcreadiness.auditor.score.ModuleResolver;
import org.pqcreadiness.auditor.score.ScoringEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end: scan a small fixture tree, score it, and render both report formats. */
class EndToEndReportTest {

    @TempDir
    Path root;

    @Test
    void producesJsonAndMarkdownReports() throws IOException {
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        Path src = Files.createDirectories(root.resolve("src/main/java/demo"));
        Files.writeString(src.resolve("Crypto.java"), """
                package demo;
                import java.security.KeyPairGenerator;
                import java.security.Signature;
                import java.security.interfaces.RSAPublicKey;
                public class Crypto {
                    RSAPublicKey key;
                    void m() throws Exception {
                        KeyPairGenerator.getInstance("RSA");
                        Signature.getInstance("SHA256withECDSA");
                    }
                }
                """);

        ScanResult scan = new Scanner().scan(root);
        ReadinessReport report = new ScoringEngine("test")
                .score("fixture", scan, new ModuleResolver(root));

        // 1 KPG + 1 Signature + 1 type-coupling (field) = 3 findings.
        assertTrue(report.totalFindings() >= 3, "findings: " + report.totalFindings());

        String json = new JsonReportWriter().toJson(report);
        assertNotNull(json);
        assertTrue(json.contains("JCA-KPG-RSA"));
        assertTrue(json.contains("\"scoreModel\" : \"v0\""));

        String md = new MarkdownReportWriter().toMarkdown(report);
        assertTrue(md.contains("# PQC Migration Readiness Report: fixture"));
        assertTrue(md.contains("Module ranking"));
        assertTrue(md.contains("harvest-now-decrypt-later"));

        // Writers persist to disk without error.
        Path out = root.resolve("out");
        new JsonReportWriter().write(report, out.resolve("r.json"));
        new MarkdownReportWriter().write(report, out.resolve("r.md"));
        assertTrue(Files.exists(out.resolve("r.json")));
        assertTrue(Files.exists(out.resolve("r.md")));
    }
}
