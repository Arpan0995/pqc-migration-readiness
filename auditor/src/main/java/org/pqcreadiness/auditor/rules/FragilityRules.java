package org.pqcreadiness.auditor.rules;

import java.util.Locale;
import java.util.Set;

/**
 * Constants for the structural fragility indicators (wave 3) from
 * {@code docs/research/02-detection-rule-catalog.md} &sect;4. These are the "novel
 * signal" the difficulty hypothesis rests on: code that assumes classical crypto
 * sizes, formats, or protocol versions is disproportionately expensive to migrate
 * because PQC keys/signatures are 1&ndash;3 orders of magnitude larger.
 */
public final class FragilityRules {

    private FragilityRules() {
    }

    /**
     * Sentinel buffer sizes that correspond to classical key/signature/curve widths
     * (F1). A {@code new byte[C]} or {@code ByteBuffer.allocate(C)} sized to one of
     * these near crypto code will not hold a multi-KB PQC artifact.
     */
    public static final Set<Integer> SENTINEL_SIZES = Set.of(
            32, 64, 65, 70, 72, 91, 128, 256, 294, 384, 512);

    /** KeyStore types that indicate persisted key material / certificate chains (F6). */
    public static final Set<String> KEYSTORE_TYPES = Set.of(
            "JKS", "PKCS12", "JCEKS", "BKS", "BCFKS");

    /** Encoded key-spec types indicating serialized key material (F6). */
    public static final Set<String> KEY_SPEC_TYPES = Set.of(
            "X509EncodedKeySpec", "PKCS8EncodedKeySpec");

    /** TLS/SSL context receivers whose {@code getInstance} pins a protocol version. */
    public static final Set<String> TLS_CONTEXT_RECEIVERS = Set.of(
            "SSLContext", "SSLEngine");

    /** Pinned (legacy) TLS/SSL protocol versions that foreclose negotiation (F3). */
    public static final Set<String> PINNED_TLS_VERSIONS = Set.of(
            "TLSV1", "TLSV1.1", "TLSV1.2", "SSLV3", "SSLV2HELLO");

    /** Methods that pin the enabled TLS suites/protocols to a fixed list (F3). */
    public static final Set<String> TLS_PINNING_METHODS = Set.of(
            "setEnabledCipherSuites", "setEnabledProtocols");

    /** {@code ByteBuffer} factory methods whose fixed argument is a buffer size (F1). */
    public static final Set<String> BYTE_BUFFER_ALLOCATORS = Set.of(
            "allocate", "allocateDirect");

    public static boolean isPinnedTlsVersion(String raw) {
        return PINNED_TLS_VERSIONS.contains(raw.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean isKeystoreType(String raw) {
        return KEYSTORE_TYPES.contains(raw.trim().toUpperCase(Locale.ROOT));
    }
}
