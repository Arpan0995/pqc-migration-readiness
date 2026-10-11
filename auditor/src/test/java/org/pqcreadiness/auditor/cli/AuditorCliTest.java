package org.pqcreadiness.auditor.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditorCliTest {

    @TempDir
    Path root;

    @Test
    void printsSkippedFilePathAndReasonToStandardError() throws Exception {
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

        ByteArrayOutputStream capturedError = new ByteArrayOutputStream();
        PrintStream originalError = System.err;

        try (PrintStream redirectedError =
                     new PrintStream(capturedError, true, StandardCharsets.UTF_8)) {
            System.setErr(redirectedError);
            AuditorCli.main(new String[]{
                    root.toString(),
                    "--out", root.resolve("audit-out").toString(),
                    "--name", "fixture"
            });
        } finally {
            System.setErr(originalError);
        }

        String errorOutput = capturedError.toString(StandardCharsets.UTF_8);
        assertTrue(errorOutput.contains(
                "Skipped source file src/main/java/demo/Broken.java:"));
    }
}