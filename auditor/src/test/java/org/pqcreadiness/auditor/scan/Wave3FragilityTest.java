package org.pqcreadiness.auditor.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.Finding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wave-3 tests: structural fragility indicators (F1 fixed buffers, F3 protocol/suite
 * pinning, F6 persisted key material) and the scope-aware merge that attaches them to
 * co-located crypto findings. See {@code docs/research/02-detection-rule-catalog.md} &sect;4.
 */
class Wave3FragilityTest {

    @TempDir
    Path dir;

    private List<Finding> scan(String className, String source) throws IOException {
        Files.writeString(dir.resolve(className + ".java"), source);
        return new Scanner().scan(dir).findings();
    }

    private static Optional<Finding> ruled(List<Finding> findings, String ruleId) {
        return findings.stream().filter(f -> f.ruleId().equals(ruleId)).findFirst();
    }

    @Test
    void fixedByteArrayTagsColocatedSignatureWithF1() throws IOException {
        List<Finding> f = scan("A", """
                package fixture;
                import java.security.Signature;
                class A { void m() throws Exception {
                    byte[] buf = new byte[256];
                    Signature.getInstance("SHA256withRSA");
                } }
                """);
        Finding sig = ruled(f, "JCA-SIG-RSA").orElseThrow();
        assertTrue(sig.fragility().contains("F1"), "signature should inherit F1 from the fixed buffer");
    }

    @Test
    void byteBufferAllocateTagsColocatedWithF1() throws IOException {
        List<Finding> f = scan("B", """
                package fixture;
                import java.nio.ByteBuffer;
                import java.security.KeyPairGenerator;
                class B { void m() throws Exception {
                    ByteBuffer bb = ByteBuffer.allocate(64);
                    KeyPairGenerator.getInstance("EC");
                } }
                """);
        assertTrue(ruled(f, "JCA-KPG-EC").orElseThrow().fragility().contains("F1"));
    }

    @Test
    void nonSentinelBufferSizeDoesNotTagF1() throws IOException {
        List<Finding> f = scan("C", """
                package fixture;
                import java.security.KeyPairGenerator;
                class C { void m() throws Exception {
                    byte[] buf = new byte[100];
                    KeyPairGenerator.getInstance("RSA");
                } }
                """);
        assertFalse(ruled(f, "JCA-KPG-RSA").orElseThrow().fragility().contains("F1"));
    }

    @Test
    void pinnedTlsVersionIsFlaggedAndTagsColocatedWithF3() throws IOException {
        List<Finding> f = scan("D", """
                package fixture;
                import javax.net.ssl.SSLContext;
                import java.security.KeyPairGenerator;
                class D { void m() throws Exception {
                    SSLContext ctx = SSLContext.getInstance("TLSv1.2");
                    KeyPairGenerator.getInstance("EC");
                } }
                """);
        Finding pin = ruled(f, "TLS-PIN-TLSV1.2").orElseThrow();
        assertEquals(Category.TLS_CONFIG, pin.category());
        assertTrue(ruled(f, "JCA-KPG-EC").orElseThrow().fragility().contains("F3"));
    }

    @Test
    void modernTlsVersionIsNotPinned() throws IOException {
        List<Finding> f = scan("E", """
                package fixture;
                import javax.net.ssl.SSLContext;
                class E { void m() throws Exception { SSLContext.getInstance("TLSv1.3"); } }
                """);
        assertTrue(f.isEmpty(), "TLSv1.3 is current, not a legacy pin");
    }

    @Test
    void enabledCipherSuitesIsFlaggedAsPinning() throws IOException {
        List<Finding> f = scan("F", """
                package fixture;
                import javax.net.ssl.SSLSocket;
                class F { void m(SSLSocket s) {
                    s.setEnabledCipherSuites(new String[]{"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"});
                } }
                """);
        assertTrue(ruled(f, "TLS-SUITES-PINNED").isPresent());
        assertEquals(Category.TLS_CONFIG, ruled(f, "TLS-SUITES-PINNED").orElseThrow().category());
    }

    @Test
    void keystoreTagsColocatedWithF6() throws IOException {
        List<Finding> f = scan("G", """
                package fixture;
                import java.security.KeyStore;
                import java.security.KeyFactory;
                class G { void m() throws Exception {
                    KeyStore ks = KeyStore.getInstance("JKS");
                    KeyFactory.getInstance("RSA");
                } }
                """);
        // KeyStore.getInstance is not itself a scored finding; it only tags via F6.
        assertTrue(ruled(f, "JCA-KF-RSA").orElseThrow().fragility().contains("F6"));
        assertTrue(f.stream().noneMatch(x -> x.algorithm().equals("JKS")));
    }

    @Test
    void encodedKeySpecTagsColocatedWithF6() throws IOException {
        List<Finding> f = scan("H", """
                package fixture;
                import java.security.KeyFactory;
                import java.security.spec.X509EncodedKeySpec;
                class H { void m(byte[] der) throws Exception {
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
                    KeyFactory.getInstance("RSA");
                } }
                """);
        assertTrue(ruled(f, "JCA-KF-RSA").orElseThrow().fragility().contains("F6"));
    }

    @Test
    void fragilityDoesNotLeakAcrossMethodScopes() throws IOException {
        List<Finding> f = scan("I", """
                package fixture;
                import java.security.KeyPairGenerator;
                class I {
                    void buffers() { byte[] b = new byte[256]; }
                    void crypto() throws Exception { KeyPairGenerator.getInstance("RSA"); }
                }
                """);
        assertTrue(ruled(f, "JCA-KPG-RSA").orElseThrow().fragility().isEmpty(),
                "a buffer in another method must not tag this finding");
    }

    @Test
    void typeCouplingFindingDoesNotAbsorbColocatedTags() throws IOException {
        List<Finding> f = scan("J", """
                package fixture;
                import java.security.interfaces.RSAPublicKey;
                class J { void m() {
                    byte[] b = new byte[256];
                    RSAPublicKey k = null;
                } }
                """);
        Finding coupling = ruled(f, "FRAG-F4-RSAPublicKey").orElseThrow();
        // F4 finding keeps only its definitional F4, not the co-located F1.
        assertEquals(List.of("F4"), coupling.fragility());
    }
}
