package org.pqcreadiness.auditor.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ScannerRobustnessTest {

    @TempDir
    Path root;

    @Test
    void skipsUndecodableSourceAndContinuesScanning() throws IOException {
        Path good = root.resolve("Good.java");
        Files.writeString(good, """
                import java.security.KeyPairGenerator;
                class Good {
                    void generate() throws Exception {
                        KeyPairGenerator.getInstance("RSA");
                    }
                }
                """);

        Path bad = root.resolve("Bad.java");
        byte[] invalidUtf8 = "class Bad { String value = \"".getBytes(StandardCharsets.UTF_8);
        byte[] suffix = "\"; }".getBytes(StandardCharsets.UTF_8);
        byte[] source = new byte[invalidUtf8.length + 1 + suffix.length];
        System.arraycopy(invalidUtf8, 0, source, 0, invalidUtf8.length);
        source[invalidUtf8.length] = (byte) 0xff;
        System.arraycopy(suffix, 0, source, invalidUtf8.length + 1, suffix.length);
        Files.write(bad, source);

        ScanResult result = new Scanner().scan(root);

        assertEquals(1, result.findings().size());
        assertTrue(result.fileLineCounts().containsKey("Good.java"));
        assertEquals(1, result.skippedFiles().size());
        assertEquals("Bad.java", result.skippedFiles().get(0).path());
        assertFalse(result.skippedFiles().get(0).reason().isBlank());
        assertFalse(result.fileLineCounts().containsKey("Bad.java"));
    }

    @Test
    void checkVersion() throws IOException {
        Files.writeString(root.resolve("Good.java"), """
                import module java.base;
                class Good {
                    void generate(){
                        try { KeyPairGenerator.getInstance("RSA"); } catch (NoSuchAlgorithmException _) {}
                    }
                }
                """);
        ScanResult result = new Scanner().scan(root);


        assertEquals(1, result.findings().size());
        assertEquals("JCA-KPG-RSA", result.findings().get(0).ruleId());
        assertEquals(0, result.skippedFiles().size());
    }

    @Test
    void statementbeforesuperparsesandyieldsitsfinding() throws IOException {
        Files.writeString(root.resolve("Good.java"), """
                import module java.base;
                
                class Good {
                    Good() throws NoSuchAlgorithmException {
                        KeyPairGenerator.getInstance("RSA");
                        super();
                    }
                }
                """);
        ScanResult result = new Scanner().scan(root);


        assertEquals(1, result.findings().size());
        assertEquals("JCA-KPG-RSA", result.findings().get(0).ruleId());
        assertEquals(0, result.skippedFiles().size());
    }

    @Test
    void fileSystemExceptionReasonDoesNotExposeAbsolutePath() {
        String absolutePath = root.resolve("Bad.java").toAbsolutePath().toString();
        AccessDeniedException exception = new AccessDeniedException(absolutePath, null, "Access denied");

        String reason = Scanner.exceptionReason(exception, "Bad.java");

        assertEquals("AccessDeniedException: Access denied", reason);
        assertFalse(reason.contains(absolutePath));
    }
}
