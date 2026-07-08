package org.pqcreadiness.auditor.scan;

import org.pqcreadiness.auditor.model.Finding;

import java.util.List;

/**
 * Per-file collector passed to the detection visitor. It gathers primary findings and
 * fragility signals, each tagged with the key of its enclosing scope, so a later merge
 * step can attach co-located fragility indicators to the crypto findings they qualify.
 */
final class ScanContext {

    /** A primary finding together with the key of the scope it was found in. */
    record ScopedFinding(Finding finding, String scopeKey) {
    }

    /** A fragility indicator observed in a given scope, e.g. {@code F1} in {@code callable@12:3}. */
    record Signal(String indicator, String scopeKey) {
    }

    private final String file;
    private final List<ScopedFinding> findings;
    private final List<Signal> signals;

    ScanContext(String file, List<ScopedFinding> findings, List<Signal> signals) {
        this.file = file;
        this.findings = findings;
        this.signals = signals;
    }

    String file() {
        return file;
    }

    void addFinding(Finding finding, String scopeKey) {
        findings.add(new ScopedFinding(finding, scopeKey));
    }

    void addSignal(String indicator, String scopeKey) {
        signals.add(new Signal(indicator, scopeKey));
    }
}
