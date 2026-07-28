package org.pqcreadiness.auditor.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Directory-name exclusion: build output must be prunable without changing defaults. */
class ScannerExclusionTest {

    @TempDir
    Path root;

    private static final String VULNERABLE = """
            import java.security.KeyPairGenerator;
            class C { void m() throws Exception { KeyPairGenerator.getInstance("RSA"); } }
            """;

    @Test
    void excludesNamedDirectoriesAtAnyDepth() throws IOException {
        Files.createDirectories(root.resolve("src/main/java"));
        Files.writeString(root.resolve("src/main/java/Real.java"), VULNERABLE);
        Files.createDirectories(root.resolve("target/generated-sources"));
        Files.writeString(root.resolve("target/generated-sources/Gen.java"), VULNERABLE);
        Files.createDirectories(root.resolve("module/target"));
        Files.writeString(root.resolve("module/target/Nested.java"), VULNERABLE);

        ScanResult all = new Scanner().scan(root);
        assertEquals(3, all.fileLineCounts().size(), "default scanner keeps its old behavior");

        ScanResult filtered = new Scanner(Set.of("target")).scan(root);
        assertEquals(1, filtered.fileLineCounts().size());
        assertTrue(filtered.fileLineCounts().containsKey("src/main/java/Real.java"));
        assertEquals(1, filtered.findings().size());
    }
}
