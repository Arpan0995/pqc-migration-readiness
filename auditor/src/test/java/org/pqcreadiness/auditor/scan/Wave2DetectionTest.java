package org.pqcreadiness.auditor.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.Finding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wave-2 detection tests: extended JCA entry points (Signature, KeyAgreement,
 * KeyFactory) and concrete key-type coupling (F4). See
 * {@code docs/research/02-detection-rule-catalog.md} &sect;3&ndash;4.
 */
class Wave2DetectionTest {

    @TempDir
    Path dir;

    private List<Finding> scanClass(String className, String source) throws IOException {
        Files.writeString(dir.resolve(className + ".java"), source);
        return new Scanner().scan(dir).findings();
    }

    @Test
    void flagsEcdsaSignature() throws IOException {
        List<Finding> f = scanClass("S1", """
                package fixture;
                import java.security.Signature;
                class S1 { void m() throws Exception { Signature.getInstance("SHA256withECDSA"); } }
                """);
        assertEquals(1, f.size());
        assertEquals("JCA-SIG-ECDSA", f.get(0).ruleId());
        assertEquals(Category.SIGNATURE, f.get(0).category());
    }

    @Test
    void flagsRsaPssSignature() throws IOException {
        List<Finding> f = scanClass("S2", """
                package fixture;
                import java.security.Signature;
                class S2 { void m() throws Exception { Signature.getInstance("RSASSA-PSS"); } }
                """);
        assertEquals(1, f.size());
        assertEquals("JCA-SIG-RSA", f.get(0).ruleId());
    }

    @Test
    void flagsEd25519Signature() throws IOException {
        List<Finding> f = scanClass("S3", """
                package fixture;
                import java.security.Signature;
                class S3 { void m() throws Exception { Signature.getInstance("Ed25519"); } }
                """);
        assertEquals(1, f.size());
        assertEquals("JCA-SIG-ED25519", f.get(0).ruleId());
    }

    @Test
    void flagsKeyAgreement() throws IOException {
        List<Finding> f = scanClass("KA", """
                package fixture;
                import javax.crypto.KeyAgreement;
                class KA { void m() throws Exception { KeyAgreement.getInstance("ECDH"); } }
                """);
        assertEquals(1, f.size());
        assertEquals("JCA-KA-ECDH", f.get(0).ruleId());
        assertEquals(Category.KEY_ESTABLISHMENT, f.get(0).category());
    }

    @Test
    void flagsKeyFactory() throws IOException {
        List<Finding> f = scanClass("KF", """
                package fixture;
                import java.security.KeyFactory;
                class KF { void m() throws Exception { KeyFactory.getInstance("RSA"); } }
                """);
        assertEquals(1, f.size());
        assertEquals("JCA-KF-RSA", f.get(0).ruleId());
    }

    @Test
    void flagsConcreteKeyTypeCoupling() throws IOException {
        List<Finding> f = scanClass("TC", """
                package fixture;
                import java.security.interfaces.RSAPublicKey;
                class TC {
                    RSAPublicKey field;
                    RSAPublicKey use(RSAPublicKey in) { return in; }
                }
                """);
        // field type + parameter type + return type = 3 occurrences
        assertEquals(3, f.size());
        assertTrue(f.stream().allMatch(x -> x.ruleId().equals("FRAG-F4-RSAPublicKey")));
        assertTrue(f.stream().allMatch(x -> x.fragility().contains("F4")));
        assertTrue(f.stream().allMatch(x -> x.category() == Category.TYPE_COUPLING));
    }

    @Test
    void doesNotFlagMacOrDigest() throws IOException {
        List<Finding> f = scanClass("N", """
                package fixture;
                import javax.crypto.Mac;
                import java.security.MessageDigest;
                class N { void m() throws Exception {
                    Mac.getInstance("HmacSHA256");
                    MessageDigest.getInstance("SHA-256");
                } }
                """);
        assertTrue(f.isEmpty());
    }

    @Test
    void doesNotFlagSymmetricSignatureLikeName() throws IOException {
        // HmacSHA256 is not a Signature algorithm; ensure signatureFamily rejects it.
        List<Finding> f = scanClass("N2", """
                package fixture;
                import java.security.Signature;
                class N2 { void m() throws Exception { Signature.getInstance("SHA256withXYZ"); } }
                """);
        assertTrue(f.isEmpty());
    }
}
