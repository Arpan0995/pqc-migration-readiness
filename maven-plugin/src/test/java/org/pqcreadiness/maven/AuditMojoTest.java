package org.pqcreadiness.maven;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mojo plumbing: parameter wiring, target exclusion, and the three report files. */
class AuditMojoTest {

    @TempDir
    Path project;

    private static void set(AuditMojo mojo, String field, Object value) throws Exception {
        Field f = AuditMojo.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(mojo, value);
    }

    @Test
    void writesThreeReportsAndSkipsBuildOutput() throws Exception {
        Files.writeString(project.resolve("pom.xml"), "<project/>");
        Path src = Files.createDirectories(project.resolve("src/main/java/demo"));
        Files.writeString(src.resolve("Crypto.java"), """
                package demo;
                import java.security.KeyPairGenerator;
                public class Crypto {
                    void m() throws Exception { KeyPairGenerator.getInstance("RSA"); }
                }
                """);
        // Decoy in build output: must not be audited.
        Path target = Files.createDirectories(project.resolve("target/generated-sources"));
        Files.writeString(target.resolve("Gen.java"), """
                import java.security.Signature;
                class Gen { void m() throws Exception { Signature.getInstance("SHA256withECDSA"); } }
                """);

        Path out = project.resolve("target/pqc-readiness");
        AuditMojo mojo = new AuditMojo();
        set(mojo, "sourceRoot", project.toFile());
        set(mojo, "outputDirectory", out.toFile());
        set(mojo, "name", "fixture");
        set(mojo, "pluginVersion", "test");
        mojo.execute();

        assertTrue(Files.exists(out.resolve("readiness-report.json")));
        assertTrue(Files.exists(out.resolve("readiness-report.md")));
        assertTrue(Files.exists(out.resolve("readiness-report.sarif")));

        JsonNode report = new ObjectMapper().readTree(out.resolve("readiness-report.json").toFile());
        assertEquals("fixture", report.path("codebase").asText());
        assertEquals("test", report.path("auditorVersion").asText());
        assertEquals(1, report.path("filesScanned").asInt(), "decoy in target/ must be pruned");
        assertEquals(1, report.path("totalFindings").asInt());
        assertFalse(report.toString().contains("Gen.java"));
    }

    @Test
    void skipShortCircuits() throws Exception {
        AuditMojo mojo = new AuditMojo();
        set(mojo, "skip", true);
        mojo.execute(); // must not touch the (nonexistent) source root
    }
}
