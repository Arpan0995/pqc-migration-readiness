package org.pqcreadiness.auditor.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pqcreadiness.auditor.model.Confidence;
import org.pqcreadiness.auditor.model.Finding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Constant-propagation detection: {@code getInstance(alg)} resolved through a local
 * variable or a {@code final} field initialised to a literal, at MEDIUM confidence.
 * See {@code docs/research/02-detection-rule-catalog.md} §5.
 */
class ConstantPropagationTest {

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
    void resolvesLocalVariableToLiteral() throws IOException {
        List<Finding> f = scan("A", """
                package fixture;
                import java.security.KeyPairGenerator;
                class A { void m() throws Exception {
                    String alg = "RSA";
                    KeyPairGenerator.getInstance(alg);
                } }
                """);
        Finding finding = ruled(f, "JCA-KPG-RSA").orElseThrow();
        assertEquals(Confidence.MEDIUM, finding.confidence(), "constant-propagated -> MEDIUM");
    }

    @Test
    void resolvesStaticFinalField() throws IOException {
        List<Finding> f = scan("B", """
                package fixture;
                import java.security.Signature;
                class B {
                    private static final String SIG_ALG = "SHA256withECDSA";
                    void m() throws Exception { Signature.getInstance(SIG_ALG); }
                }
                """);
        Finding finding = ruled(f, "JCA-SIG-ECDSA").orElseThrow();
        assertEquals(Confidence.MEDIUM, finding.confidence());
    }

    @Test
    void resolvesConstantConcatenation() throws IOException {
        List<Finding> f = scan("C", """
                package fixture;
                import java.security.Signature;
                class C {
                    private static final String WITH = "withRSA";
                    void m() throws Exception { Signature.getInstance("SHA256" + WITH); }
                }
                """);
        assertTrue(ruled(f, "JCA-SIG-RSA").isPresent());
        assertEquals(Confidence.MEDIUM, ruled(f, "JCA-SIG-RSA").orElseThrow().confidence());
    }

    @Test
    void directLiteralStaysHighConfidence() throws IOException {
        List<Finding> f = scan("D", """
                package fixture;
                import java.security.KeyPairGenerator;
                class D { void m() throws Exception { KeyPairGenerator.getInstance("RSA"); } }
                """);
        assertEquals(Confidence.HIGH, ruled(f, "JCA-KPG-RSA").orElseThrow().confidence());
    }

    @Test
    void resolvesConstantTlsVersionPin() throws IOException {
        List<Finding> f = scan("E", """
                package fixture;
                import javax.net.ssl.SSLContext;
                class E {
                    private static final String PROTO = "TLSv1.2";
                    void m() throws Exception { SSLContext.getInstance(PROTO); }
                }
                """);
        assertTrue(ruled(f, "TLS-PIN-TLSV1.2").isPresent());
    }

    @Test
    void doesNotResolveReassignedVariable() throws IOException {
        List<Finding> f = scan("F", """
                package fixture;
                import java.security.KeyPairGenerator;
                class F { void m(boolean b) throws Exception {
                    String alg = "RSA";
                    if (b) { alg = "AES"; }
                    KeyPairGenerator.getInstance(alg);
                } }
                """);
        assertTrue(f.stream().noneMatch(x -> x.ruleId().startsWith("JCA-KPG")),
                "a reassigned variable is not a safe constant and must not be resolved");
    }

    @Test
    void doesNotResolveNonFinalField() throws IOException {
        List<Finding> f = scan("G", """
                package fixture;
                import java.security.KeyPairGenerator;
                class G {
                    private String alg = "RSA";
                    void m() throws Exception { KeyPairGenerator.getInstance(alg); }
                }
                """);
        assertTrue(f.stream().noneMatch(x -> x.ruleId().startsWith("JCA-KPG")),
                "a non-final field may change; not resolved");
    }

    @Test
    void doesNotResolveUnknownIdentifier() throws IOException {
        // Algorithm from a method parameter (genuinely dynamic) is not resolvable.
        List<Finding> f = scan("H", """
                package fixture;
                import java.security.KeyPairGenerator;
                class H { void m(String alg) throws Exception { KeyPairGenerator.getInstance(alg); } }
                """);
        assertTrue(f.stream().noneMatch(x -> x.ruleId().startsWith("JCA-KPG")));
    }

    @Test
    void resolvesCrossFileConstant() throws IOException {
        // A constant declared in another file (no classpath) resolved via the
        // project-wide constant index — the pattern real libraries actually use,
        // e.g. mina-sshd's KeyPairGenerator.getInstance(KeyUtils.RSA_ALGORITHM).
        Files.writeString(dir.resolve("CryptoConstants.java"), """
                package fixture;
                public final class CryptoConstants {
                    public static final String RSA_ALG = "RSA";
                }
                """);
        Files.writeString(dir.resolve("User.java"), """
                package fixture;
                import java.security.KeyPairGenerator;
                class User { void m() throws Exception {
                    KeyPairGenerator.getInstance(CryptoConstants.RSA_ALG);
                } }
                """);
        List<Finding> findings = new Scanner().scan(dir).findings();
        Finding f = ruled(findings, "JCA-KPG-RSA").orElseThrow();
        assertEquals(Confidence.MEDIUM, f.confidence());
    }

    @Test
    void resolvedConstantStillNotFlaggedIfAlgorithmSafe() throws IOException {
        List<Finding> f = scan("I", """
                package fixture;
                import java.security.KeyPairGenerator;
                class I {
                    private static final String ALG = "ML-KEM";
                    void m() throws Exception { KeyPairGenerator.getInstance(ALG); }
                }
                """);
        assertTrue(f.isEmpty(), "resolving a non-vulnerable algorithm still yields no finding");
    }
}
