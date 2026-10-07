package org.pqcreadiness.auditor.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pqcreadiness.auditor.model.ReadinessReport;
import org.pqcreadiness.auditor.scan.ScanResult;
import org.pqcreadiness.auditor.scan.Scanner;
import org.pqcreadiness.auditor.score.ModuleResolver;
import org.pqcreadiness.auditor.score.ScoringEngine;
import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.Confidence;
import org.pqcreadiness.auditor.model.EffortTier;
import org.pqcreadiness.auditor.model.FileReport;
import org.pqcreadiness.auditor.model.Finding;
import org.pqcreadiness.auditor.model.ModuleReport;
import org.pqcreadiness.auditor.model.SkippedFile;

import java.util.List;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
                assertTrue(json.contains("\"migrationPlan\""));
                assertTrue(json.contains("\"timeModel\" : \"t0\""));

                String md = new MarkdownReportWriter().toMarkdown(report);
                assertTrue(md.contains("# PQC Migration Readiness Report: fixture"));
                assertTrue(md.contains("Module ranking"));
                assertTrue(md.contains("harvest-now-decrypt-later"));
                assertTrue(md.contains("Migration plan at a glance"));
                assertTrue(md.contains("Estimated effort for one engineer"));

                // Writers persist to disk without error.
                Path out = root.resolve("out");
                new JsonReportWriter().write(report, out.resolve("r.json"));
                new MarkdownReportWriter().write(report, out.resolve("r.md"));
                assertTrue(Files.exists(out.resolve("r.json")));
                assertTrue(Files.exists(out.resolve("r.md")));
        }

        @Test
        void limitsSkippedFilesInMarkdown() {
            List<SkippedFile> skippedFiles = java.util.stream.IntStream.rangeClosed(1, 22)
                    .mapToObj(index -> new SkippedFile("Broken" + index + ".java", "parse error"))
                    .toList();

            ReadinessReport report = new ReadinessReport(
                    "fixture", "test", "v0", "now",
                    0, 0, skippedFiles.size(), skippedFiles, List.of());

            String markdown = new MarkdownReportWriter().toMarkdown(report);

            long listedFiles = markdown.lines()
                    .filter(line -> line.startsWith("- `Broken"))
                    .count();

            assertEquals(20L, listedFiles);
            assertTrue(markdown.contains("+2 more skipped file(s) not shown."));
        }

        @Test
        void ranksExpensiveHotspotBeforeApplyingCap() throws IOException {
                Files.writeString(root.resolve("pom.xml"), "<project/>");
                Path src = Files.createDirectories(root.resolve("src/main/java/demo"));

                StringBuilder big = new StringBuilder("""
                                package demo;
                                import java.security.interfaces.RSAPublicKey;
                                class Big {
                                """);
                for (int i = 0; i < 16; i++) {
                        big.append("    RSAPublicKey key").append(i).append(";\n");
                }
                big.append("}\n");
                Files.writeString(src.resolve("Big.java"), big.toString());

                Files.writeString(src.resolve("Hot.java"), """
                                package demo;
                                import java.security.spec.X509EncodedKeySpec;
                                import javax.crypto.Cipher;
                                class Hot {
                                    void m(byte[] der) throws Exception {
                                        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
                                        byte[] buffer = new byte[256];
                                        Cipher.getInstance("RSA/ECB/PKCS1Padding");
                                    }
                                }
                                """);

                ScanResult scan = new Scanner().scan(root);
                ReadinessReport report = new ScoringEngine("test")
                                .score("fixture", scan, new ModuleResolver(root));

                assertEquals(17, report.totalFindings());
                assertEquals(1, report.modules().size());

                String md = new MarkdownReportWriter().toMarkdown(report);
                List<String> rows = md.lines()
                                .filter(line -> line.startsWith("| `")
                                                && line.contains(".java:"))
                                .toList();

                assertEquals(15, rows.size(), "The hotspot cap must remain 15");
                double previousDifficulty = Double.POSITIVE_INFINITY;
                for (String row : rows) {
                        double difficulty = Double.parseDouble(row.split("\\|")[3].trim());
                        assertTrue(difficulty <= previousDifficulty,
                                        "Difficulty must not increase down the hotspot table: " + row);
                        previousDifficulty = difficulty;
                }
                assertTrue(rows.get(0).contains("`JCA-CIPHER-RSA` | 9.0 |"),
                                "The highest-difficulty finding must come first: " + rows.get(0));
                assertTrue(md.contains("_2 more finding(s) not shown._"));
        }

        @Test
        void excludesSyntaxErrorFileFromScannedFilesAndModuleLoc() throws IOException {
                Files.writeString(root.resolve("pom.xml"), "<project/>");
                Path src = Files.createDirectories(root.resolve("src/main/java/demo"));

                Files.writeString(src.resolve("Good.java"), """
                                package demo;
                                import java.security.KeyPairGenerator;
                                class Good {
                                    void generate() throws Exception {
                                        KeyPairGenerator.getInstance("RSA");
                                    }
                                }
                                """);

                Files.writeString(src.resolve("Broken.java"), """
                                package demo;
                                class Broken {
                                    void incomplete() {
                                """);

                ScanResult scan = new Scanner().scan(root);
                ReadinessReport report = new ScoringEngine("test")
                                .score("fixture", scan, new ModuleResolver(root));

                assertEquals(1, report.filesScanned());
                assertEquals(1, report.filesSkipped());
                assertEquals(1, report.modules().size());
                assertEquals("src/main/java/demo/Broken.java", report.skippedFiles().get(0).path());
                assertEquals(7, report.modules().get(0).loc());

                assertEquals(1, scan.skippedFiles().size());
                assertEquals("src/main/java/demo/Broken.java", scan.skippedFiles().get(0).path());

            String reason = scan.skippedFiles().get(0).reason();
            assertTrue(reason.startsWith("Parse error"), reason);
            assertTrue(reason.contains("<EOF>"), reason);

                String markdown = new MarkdownReportWriter().toMarkdown(report);
                assertTrue(markdown.contains("## Files that could not be parsed"));
                assertTrue(markdown.contains("`src/main/java/demo/Broken.java`"));

            String escapedReason = reason.replace("&", "&amp;")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;")
                            .replace("`", "\\`");
            assertTrue(markdown.contains(escapedReason));
            assertTrue(markdown.contains("&lt;EOF&gt;"));
        }

        @Test
        void ordersEqualDifficultyHotspotsByPathLineAndColumn() {
                FileReport b = new FileReport("B.java", 1.0, List.of(
                                typeCouplingFinding("B.java", 1, 1)));
                FileReport a = new FileReport("A.java", 3.0, List.of(
                                typeCouplingFinding("A.java", 10, 20),
                                typeCouplingFinding("A.java", 10, 5),
                                typeCouplingFinding("A.java", 2, 30)));

                ModuleReport module = new ModuleReport(
                                "demo", 20, 4.0, EffortTier.forScore(4.0),
                                0.0, 0, List.of(b, a));
                ReadinessReport report = new ReadinessReport(
                                "fixture", "test", "v0", "2026-09-26T00:00:00Z",
                                4, 2, 0, List.of(module));

                List<String> sites = new MarkdownReportWriter().toMarkdown(report)
                                .lines()
                                .filter(line -> line.startsWith("| `")
                                                && line.contains(".java:"))
                                .map(line -> line.split("`", 3)[1])
                                .toList();

                assertEquals(List.of(
                                "A.java:2:30",
                                "A.java:10:5",
                                "A.java:10:20",
                                "B.java:1:1"), sites);
        }

        private static Finding typeCouplingFinding(String file, int line, int column) {
                return new Finding(
                                "FRAG-F4-RSAPublicKey", file, line, column,
                                "RSAPublicKey", "RSA", Category.TYPE_COUPLING,
                                Confidence.HIGH, List.of("F4"), "RSAPublicKey key;");
        }
}
