package org.pqcreadiness.agility.audit;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects {@link AuditEvent}s and renders them as JSON Lines (one flat JSON object per
 * line). JSON is hand-rendered to keep this module free of a JSON dependency; the shape
 * is small and fixed. Thread-safety is provided by simple synchronisation, since the
 * agility layer may be exercised concurrently.
 */
public final class AuditLog {

    private final List<AuditEvent> events = new ArrayList<>();

    public synchronized void record(AuditEvent event) {
        events.add(event);
    }

    public synchronized List<AuditEvent> events() {
        return List.copyOf(events);
    }

    /** Render all recorded events as JSON Lines. */
    public synchronized String toJsonl() {
        StringBuilder sb = new StringBuilder();
        for (AuditEvent e : events) {
            sb.append('{')
                    .append(field("ts", e.timestamp())).append(',')
                    .append(field("intent", e.intent())).append(',')
                    .append(field("mode", e.mode())).append(',')
                    .append(field("suite", e.suiteId())).append(',')
                    .append(field("peerOffer", e.peerOffer())).append(',')
                    .append(field("outcome", e.outcome())).append(',')
                    .append("\"durationNanos\":").append(e.durationNanos())
                    .append('}').append('\n');
        }
        return sb.toString();
    }

    private static String field(String key, String value) {
        return '"' + key + "\":\"" + escape(value == null ? "" : value) + '"';
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
