package org.pqcreadiness.auditor.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.Confidence;
import org.pqcreadiness.auditor.model.Finding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wave-1 detector tests, exercising the full scan path (file walk, parse, visit)
 * against constructed fixtures. Covers the positive/negative and edge cases called
 * out in {@code docs/research/02-detection-rule-catalog.md} &sect;2.
 */
class Wave1ScannerTest {

    @TempDir
    Path dir;

    private List<Finding> scan(String className, String body) throws IOException {
        String source = """
                package fixture;
                import javax.crypto.Cipher;
                import java.security.KeyPairGenerator;
                class %s {
                    void m() throws Exception {
                %s
                    }
                }
                """.formatted(className, body);
        Files.writeString(dir.resolve(className + ".java"), source);
        return new Scanner().scan(dir).findings();
    }

    @Test
    void flagsRsaCipherTransformation() throws IOException {
        List<Finding> findings = scan("A",
                "        Cipher c = Cipher.getInstance(\"RSA/ECB/OAEPWithSHA-256AndMGF1Padding\");");
        assertEquals(1, findings.size());
        Finding f = findings.get(0);
        assertEquals("JCA-CIPHER-RSA", f.ruleId());
        assertEquals("RSA", f.algorithm());
        assertEquals(Category.KEY_ESTABLISHMENT, f.category());
        assertEquals(Confidence.HIGH, f.confidence());
        assertEquals("A.java", f.file());
    }

    @Test
    void flagsRsaKeyPairGenerator() throws IOException {
        List<Finding> findings = scan("B", "        KeyPairGenerator.getInstance(\"RSA\");");
        assertEquals(1, findings.size());
        assertEquals("JCA-KPG-RSA", findings.get(0).ruleId());
        assertEquals(Category.KEYGEN, findings.get(0).category());
    }

    @Test
    void flagsEllipticCurveKeyPairGenerator() throws IOException {
        List<Finding> findings = scan("C", "        KeyPairGenerator.getInstance(\"EC\");");
        assertEquals(1, findings.size());
        assertEquals("JCA-KPG-EC", findings.get(0).ruleId());
    }

    @Test
    void flagsDiffieHellmanShortName() throws IOException {
        List<Finding> dh = scan("D1", "        KeyPairGenerator.getInstance(\"DH\");");
        assertEquals(1, dh.size());
        assertEquals("JCA-KPG-DH", dh.get(0).ruleId());
    }

    @Test
    void flagsDiffieHellmanSpelledOut() throws IOException {
        List<Finding> spelled = scan("D2", "        KeyPairGenerator.getInstance(\"DiffieHellman\");");
        assertEquals(1, spelled.size());
        assertEquals("JCA-KPG-DIFFIEHELLMAN", spelled.get(0).ruleId());
    }

    @Test
    void algorithmMatchingIsCaseInsensitive() throws IOException {
        List<Finding> findings = scan("E", "        KeyPairGenerator.getInstance(\"rsa\");");
        assertEquals(1, findings.size());
        assertEquals("JCA-KPG-RSA", findings.get(0).ruleId());
    }

    @Test
    void resolvesFullyQualifiedReceiver() throws IOException {
        List<Finding> findings = scan("F",
                "        javax.crypto.Cipher.getInstance(\"RSA\");");
        assertEquals(1, findings.size());
        assertEquals("JCA-CIPHER-RSA", findings.get(0).ruleId());
    }

    @Test
    void doesNotFlagSymmetricCipher() throws IOException {
        assertTrue(scan("G", "        Cipher.getInstance(\"AES/GCM/NoPadding\");").isEmpty());
    }

    @Test
    void doesNotFlagPostQuantumAlgorithm() throws IOException {
        assertTrue(scan("H", "        KeyPairGenerator.getInstance(\"ML-KEM\");").isEmpty());
    }

    @Test
    void resolvesLocalConstantArgument() throws IOException {
        // Constant propagation now resolves a local variable assigned a literal.
        // (Detailed confidence/edge-case coverage lives in ConstantPropagationTest.)
        List<Finding> findings = scan("I",
                "        String alg = \"RSA\";\n        KeyPairGenerator.getInstance(alg);");
        assertEquals(1, findings.size());
        assertEquals("JCA-KPG-RSA", findings.get(0).ruleId());
    }

    @Test
    void doesNotFlagUnrelatedGetInstance() throws IOException {
        assertTrue(scan("J",
                "        java.util.Calendar.getInstance();").isEmpty());
    }

    @Test
    void reportsLineNumber() throws IOException {
        List<Finding> findings = scan("K", "        KeyPairGenerator.getInstance(\"DSA\");");
        assertEquals(1, findings.size());
        // package(1) import(2) import(3) class(4) m()(5) body-line-1(6)
        assertEquals(6, findings.get(0).line());
    }

    @Test
    void scansMultipleFilesWithRelativePaths() throws IOException {
        Path pkg = Files.createDirectories(dir.resolve("com/example"));
        Files.writeString(pkg.resolve("One.java"),
                "package com.example; import java.security.KeyPairGenerator;"
                        + " class One { void m() throws Exception { KeyPairGenerator.getInstance(\"RSA\"); } }");
        Files.writeString(pkg.resolve("Two.java"),
                "package com.example; import javax.crypto.Cipher;"
                        + " class Two { void m() throws Exception { Cipher.getInstance(\"ECIES\"); } }");
        List<Finding> findings = new Scanner().scan(dir).findings();
        assertEquals(2, findings.size());
        assertTrue(findings.stream().anyMatch(f -> f.file().equals("com/example/One.java")));
        assertTrue(findings.stream().anyMatch(f -> f.file().equals("com/example/Two.java")));
    }
}
