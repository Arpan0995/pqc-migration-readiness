package org.pqcreadiness.auditor.report;

import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.Finding;

/**
 * Plain-language rationale for a finding: what it is and why it costs effort to
 * migrate. This is what turns the report from a linter's list into a readiness
 * assessment — every hotspot says <em>why</em> it is expensive, not just where it is.
 */
public final class Explanations {

    private Explanations() {
    }

    /** One-line reason a finding matters for a PQC migration. */
    public static String why(Finding finding) {
        StringBuilder reason = new StringBuilder(categoryReason(finding));
        for (String indicator : finding.fragility()) {
            // A type-coupling finding's category reason already explains F4; don't repeat it.
            if (finding.category() == Category.TYPE_COUPLING && indicator.equals("F4")) {
                continue;
            }
            String note = fragilityNote(indicator);
            if (note != null) {
                reason.append(' ').append(note);
            }
        }
        return reason.toString();
    }

    private static String categoryReason(Finding finding) {
        return switch (finding.category()) {
            case KEY_ESTABLISHMENT -> "Quantum-vulnerable key establishment/encryption ("
                    + finding.algorithm() + "); confidentiality is at harvest-now-decrypt-later risk today.";
            case SIGNATURE -> "Quantum-vulnerable signature (" + finding.algorithm()
                    + "); PQC signatures are much larger and may break size assumptions.";
            case KEYGEN -> "Generates a quantum-vulnerable key pair (" + finding.algorithm()
                    + "); the algorithm choice must move to a PQC or hybrid scheme.";
            case TLS_CONFIG -> "Pins classical TLS parameters that a hybrid migration must renegotiate.";
            case JOSE -> "Pins a classical JOSE/JWT signature algorithm (" + finding.algorithm() + ").";
            case TYPE_COUPLING -> "Couples code to the concrete key type " + finding.algorithm()
                    + "; a migration propagates through every caller of this API.";
            case INFORMATIONAL -> "Reported for inventory completeness; not counted toward the score.";
        };
    }

    private static String fragilityNote(String indicator) {
        return switch (indicator) {
            case "F1" -> "A fixed-size buffer nearby will not hold multi-KB PQC artifacts.";
            case "F2" -> "A fixed-width persistence/wire format must change (schema/protocol coordination).";
            case "F3" -> "Protocol/suite pinning forecloses the negotiation hybrid migration needs.";
            case "F4" -> "Concrete key-type coupling spreads the change across callers.";
            case "F5" -> "Algorithm is config-sourced, so this site can migrate by configuration (credit).";
            case "F6" -> "Persisted key material must be re-issued/re-encoded (data migration).";
            case "F8" -> "Crosses a third-party API boundary; migration may be blocked on an upstream release.";
            default -> null;
        };
    }
}
