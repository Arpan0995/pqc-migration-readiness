package org.pqcreadiness.auditor.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(java.util.List.of(bad), result.unparseableFiles());
    }
}
