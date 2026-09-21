package org.pqcreadiness.agility.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogTest {

    @Test
    void rendersJsonLines() {
        AuditLog log = new AuditLog();
        log.record(new AuditEvent("2026-07-08T00:00:00Z", "KEY_ESTABLISHMENT", "HYBRID",
                "KE-HYBRID-X25519-MLKEM768", "KE-CLASSICAL-X25519", "SELECTED", 1234));
        log.record(new AuditEvent("2026-07-08T00:00:01Z", "SIGNATURE", "CLASSICAL",
                "SIG-CLASSICAL-ECDSAP256", "", "DOWNGRADED", -1));

        String jsonl = log.toJsonl();
        String[] lines = jsonl.strip().split("\n");

        assertEquals(2, lines.length);
        assertTrue(lines[0].contains("\"suite\":\"KE-HYBRID-X25519-MLKEM768\""));
        assertTrue(lines[0].contains("\"durationNanos\":1234"));
        assertTrue(lines[1].contains("\"outcome\":\"DOWNGRADED\""));
        assertEquals(2, log.events().size());
    }

    @Test
    void escapesQuotesInValues() {
        AuditLog log = new AuditLog();
        log.record(new AuditEvent("t", "i", "m", "a\"b", "", "SELECTED", 0));
        assertTrue(log.toJsonl().contains("a\\\"b"));
    }

    @Test
    void escapesJsonControlCharacters() {
        AuditLog log = new AuditLog();
        log.record(new AuditEvent("t", "i", "m", "\u0000\b\f\u001f", "", "SELECTED", 0));

        String jsonl = log.toJsonl();

        assertTrue(jsonl.contains("\\u0000\\u0008\\u000c\\u001f"));
        assertTrue(jsonl.chars().noneMatch(c -> c < 0x20 && c != '\n'));
    }
}
