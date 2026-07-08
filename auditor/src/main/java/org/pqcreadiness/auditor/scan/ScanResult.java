package org.pqcreadiness.auditor.scan;

import org.pqcreadiness.auditor.model.Finding;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Output of a scan: the findings, the non-blank line count of every scanned source
 * file (used for the module-size confounder control in the validation plan), and any
 * files that failed to parse.
 *
 * @param findings          findings in file order
 * @param fileLineCounts    relative source path -> non-blank line count
 * @param unparseableFiles  files skipped because they could not be parsed
 */
public record ScanResult(
        List<Finding> findings,
        Map<String, Integer> fileLineCounts,
        List<Path> unparseableFiles) {
}
