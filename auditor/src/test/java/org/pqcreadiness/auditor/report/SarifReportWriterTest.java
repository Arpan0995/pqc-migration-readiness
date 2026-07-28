package org.pqcreadiness.auditor.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.Confidence;
import org.pqcreadiness.auditor.model.EffortTier;
import org.pqcreadiness.auditor.model.FileReport;
import org.pqcreadiness.auditor.model.Finding;
import org.pqcreadiness.auditor.model.ModuleReport;
import org.pqcreadiness.auditor.model.ReadinessReport;
import org.pqcreadiness.auditor.scan.ScanResult;
import org.pqcreadiness.auditor.scan.Scanner;
import org.pqcreadiness.auditor.score.ModuleResolver;
import org.pqcreadiness.auditor.score.ScoringEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SARIF 2.1.0 rendering: structure, rule table integrity, and level mapping. */
class SarifReportWriterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path root;

    /** End-to-end: scan a fixture tree and check the SARIF log GitHub would ingest. */
    @Test
    void producesValidSarifFromRealScan() throws IOException {
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
        assertTrue(report.totalFindings() >= 3, "findings: " + report.totalFindings());

        JsonNode log = mapper.readTree(new SarifReportWriter().toSarif(report));

        assertEquals("2.1.0", log.path("version").asText());
        assertTrue(log.path("$schema").asText().contains("sarif"));

        JsonNode run = log.path("runs").get(0);
        JsonNode driver = run.path("tool").path("driver");
        assertEquals("pqc-readiness-auditor", driver.path("name").asText());
        assertEquals("test", driver.path("version").asText());

        // Every result references a rule that exists in the driver's rule table,
        // at the index it claims.
        JsonNode rules = driver.path("rules");
        Set<String> ruleIds = new HashSet<>();
        for (JsonNode rule : rules) {
            ruleIds.add(rule.path("id").asText());
            assertFalse(rule.path("shortDescription").path("text").asText().isBlank());
        }
        assertTrue(ruleIds.contains("JCA-KPG-RSA"), "rules: " + ruleIds);

        JsonNode results = run.path("results");
        assertEquals(report.totalFindings(), results.size());
        for (JsonNode result : results) {
            String ruleId = result.path("ruleId").asText();
            assertTrue(ruleIds.contains(ruleId), "unknown rule: " + ruleId);
            assertEquals(ruleId,
                    rules.get(result.path("ruleIndex").asInt()).path("id").asText());
            assertFalse(result.path("message").path("text").asText().isBlank());

            JsonNode region = result.path("locations").get(0)
                    .path("physicalLocation").path("region");
            assertTrue(region.path("startLine").asInt() >= 1);

            String uri = result.path("locations").get(0)
                    .path("physicalLocation").path("artifactLocation").path("uri").asText();
            assertFalse(uri.contains("\\"), "URI must use forward slashes: " + uri);
        }

        // Writer persists to disk without error.
        Path out = root.resolve("out/r.sarif");
        new SarifReportWriter().write(report, out);
        assertTrue(Files.exists(out));
    }

    /** Level and severity mapping on a hand-built report covering the edge cases. */
    @Test
    void mapsLevelsAndSeverities() throws IOException {
        Finding scored = new Finding("JCA-KPG-RSA", "a/B.java", 10, 5,
                "KeyPairGenerator", "RSA", Category.KEYGEN, Confidence.HIGH,
                List.of("F1"), "KeyPairGenerator.getInstance(\"RSA\")");
        Finding lowConfidence = new Finding("JCA-SIG-DYNAMIC", "a/B.java", 20, 5,
                "Signature", "<dynamic>", Category.SIGNATURE, Confidence.LOW,
                List.of(), "Signature.getInstance(algo)");
        Finding informational = new Finding("JCA-CIPHER-AES", "a/B.java", 30, 5,
                "Cipher", "AES", Category.INFORMATIONAL, Confidence.HIGH,
                List.of(), "Cipher.getInstance(\"AES\")");
        Finding windowsPath = new Finding("JCA-KPG-EC", "win\\C.java", 1, 1,
                "KeyPairGenerator", "EC", Category.KEYGEN, Confidence.MEDIUM,
                List.of(), "KeyPairGenerator.getInstance(\"EC\")");

        ReadinessReport report = new ReadinessReport("fixture", "test", "v0", "now",
                4, 2, 0, List.of(new ModuleReport("m", 100, 8.0, EffortTier.LOW, 1.0, 3,
                        List.of(new FileReport("a/B.java", 8.0,
                                        List.of(scored, lowConfidence, informational)),
                                new FileReport("win\\C.java", 2.0, List.of(windowsPath))))));

        JsonNode log = mapper.readTree(new SarifReportWriter().toSarif(report));
        JsonNode results = log.path("runs").get(0).path("results");
        assertEquals(4, results.size());

        assertEquals("warning", results.get(0).path("level").asText());
        assertEquals("note", results.get(1).path("level").asText(),
                "LOW confidence must not raise alarms");
        assertEquals("note", results.get(2).path("level").asText(),
                "INFORMATIONAL is never an alert");
        assertEquals("win/C.java", results.get(3).path("locations").get(0)
                .path("physicalLocation").path("artifactLocation").path("uri").asText());

        // Fragility indicators ride along as result properties.
        assertEquals("F1", results.get(0).path("properties").path("fragility").get(0).asText());

        // security-severity follows the category base weight; INFORMATIONAL has none.
        JsonNode rules = log.path("runs").get(0).path("tool").path("driver").path("rules");
        for (JsonNode rule : rules) {
            String id = rule.path("id").asText();
            JsonNode severity = rule.path("properties").path("security-severity");
            switch (id) {
                case "JCA-KPG-RSA", "JCA-KPG-EC" -> assertEquals("4.0", severity.asText());
                case "JCA-SIG-DYNAMIC" -> assertEquals("6.0", severity.asText());
                case "JCA-CIPHER-AES" -> assertTrue(severity.isMissingNode());
                default -> throw new AssertionError("unexpected rule " + id);
            }
        }
    }
}
