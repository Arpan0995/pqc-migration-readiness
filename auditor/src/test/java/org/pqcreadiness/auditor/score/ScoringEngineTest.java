package org.pqcreadiness.auditor.score;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.Confidence;
import org.pqcreadiness.auditor.model.Finding;
import org.pqcreadiness.auditor.model.ModuleReport;
import org.pqcreadiness.auditor.model.ReadinessReport;
import org.pqcreadiness.auditor.scan.ScanResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests aggregation of findings into a readiness report, including module resolution. */
class ScoringEngineTest {

    @TempDir
    Path root;

    private Finding kpgRsa(String file) {
        return new Finding("JCA-KPG-RSA", file, 10, 5, "KeyPairGenerator", "RSA",
                Category.KEYGEN, Confidence.HIGH, List.of(), "KeyPairGenerator.getInstance(\"RSA\")");
    }

    @Test
    void aggregatesByBuildModuleAndRanksByScore() throws IOException {
        // Two Maven modules under the root.
        Files.createDirectories(root.resolve("mod-a"));
        Files.createDirectories(root.resolve("mod-b"));
        Files.writeString(root.resolve("mod-a/pom.xml"), "<project/>");
        Files.writeString(root.resolve("mod-b/pom.xml"), "<project/>");

        ScanResult scan = new ScanResult(
                List.of(
                        kpgRsa("mod-a/src/A1.java"),
                        kpgRsa("mod-a/src/A2.java"),
                        kpgRsa("mod-b/src/B1.java")),
                Map.of("mod-a/src/A1.java", 100, "mod-a/src/A2.java", 50, "mod-b/src/B1.java", 30),
                List.of());

        ReadinessReport report = new ScoringEngine("test")
                .score("demo", scan, new ModuleResolver(root));

        assertEquals(2, report.modules().size());
        ModuleReport top = report.modules().get(0);
        // mod-a has 2 findings across 2 files -> higher score than mod-b (1 finding).
        assertEquals("mod-a", top.name());
        assertEquals(2, top.baselineCount());
        assertEquals(150, top.loc());
        // 2 findings * base 2 = 4, spread(2 files) = 1 + 0.1*log2(3) ~= 1.1585 -> ~4.63
        assertTrue(top.score() > 4.0 && top.score() < 5.0, "score was " + top.score());

        ModuleReport second = report.modules().get(1);
        assertEquals("mod-b", second.name());
        assertEquals(1, second.baselineCount());
        assertEquals(3, report.totalFindings());
    }

    @Test
    void fallsBackToTopLevelDirWhenNoBuildFiles() {
        ScanResult scan = new ScanResult(
                List.of(kpgRsa("com/example/A.java")),
                Map.of("com/example/A.java", 42),
                List.of());
        ReadinessReport report = new ScoringEngine("test")
                .score("demo", scan, new ModuleResolver(root));
        assertEquals(1, report.modules().size());
        assertEquals("com", report.modules().get(0).name());
    }
}
