package org.pqcreadiness.auditor.scan;

import org.pqcreadiness.auditor.model.Finding;

import java.util.List;

/**
 * Per-file collector passed to detection visitors: exposes the relative file path
 * used in findings and accumulates the findings produced while visiting that file.
 */
final class ScanContext {

    private final String file;
    private final List<Finding> findings;

    ScanContext(String file, List<Finding> findings) {
        this.file = file;
        this.findings = findings;
    }

    String file() {
        return file;
    }

    void add(Finding finding) {
        findings.add(finding);
    }
}
