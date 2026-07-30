package org.pqcreadiness.auditor.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pqcreadiness.auditor.model.FileReport;
import org.pqcreadiness.auditor.model.ModuleReport;
import org.pqcreadiness.auditor.model.ReadinessReport;
import org.pqcreadiness.auditor.score.ModuleResolver;
import org.pqcreadiness.auditor.score.ScoringEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test-scope classification end to end, and the opt-in skip. */
class TestScopeScanTest {

    private static final String VULNERABLE = """
            import java.security.KeyPairGenerator;
            class C { void m() throws Exception { KeyPairGenerator.getInstance("RSA"); } }
            """;

    @TempDir
    Path root;

    private void fixture() throws IOException {
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        Files.createDirectories(root.resolve("src/main/java"));
        Files.writeString(root.resolve("src/main/java/Prod.java"), VULNERABLE);
        Files.createDirectories(root.resolve("src/test/java"));
        Files.writeString(root.resolve("src/test/java/ProdTest.java"), VULNERABLE);
    }

    private ReadinessReport score(boolean skipTests) {
        ScanResult scan = new Scanner(Set.of(), skipTests).scan(root);
        return new ScoringEngine("test").score("fixture", scan, new ModuleResolver(root));
    }

    @Test
    void classifiesTestFilesButKeepsThemByDefault() throws IOException {
        fixture();
        ReadinessReport report = score(false);

        assertEquals(2, report.filesScanned(), "default scan keeps test sources");
        assertEquals(2, report.totalFindings());

        FileReport prod = find(report, "src/main/java/Prod.java");
        FileReport test = find(report, "src/test/java/ProdTest.java");
        assertFalse(prod.testScoped());
        assertTrue(test.testScoped(), "test file must be classified");
    }

    @Test
    void skipTestsOmitsThemEntirely() throws IOException {
        fixture();
        ReadinessReport report = score(true);

        assertEquals(1, report.filesScanned());
        assertEquals(1, report.totalFindings());
        assertFalse(report.modules().stream()
                .flatMap(m -> m.files().stream())
                .anyMatch(FileReport::testScoped));
    }

    /** A module is test-scoped only when it contributes no production findings. */
    @Test
    void mixedModuleIsNotTestScoped() throws IOException {
        fixture();
        ReadinessReport report = score(false);
        ModuleReport module = report.modules().get(0);
        assertTrue(module.files().stream().anyMatch(FileReport::testScoped));
        assertFalse(module.testScoped(), "a module with production findings stays production");
    }

    @Test
    void testOnlyModuleIsTestScoped() throws IOException {
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        Files.createDirectories(root.resolve("src/test/java"));
        Files.writeString(root.resolve("src/test/java/OnlyTest.java"), VULNERABLE);

        ReadinessReport report = score(false);
        assertTrue(report.modules().get(0).testScoped());
    }

    private static FileReport find(ReadinessReport report, String path) {
        return report.modules().stream()
                .flatMap(m -> m.files().stream())
                .filter(f -> f.path().equals(path))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no file report for " + path));
    }
}
