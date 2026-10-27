package org.pqcreadiness.auditor.scan;

import org.pqcreadiness.auditor.model.Finding;
import org.pqcreadiness.auditor.model.SkippedFile;

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
 * @param skippedFiles   files skipped during scanning, with path and reason
 */
public record ScanResult(
        List<Finding> findings,
        Map<String, Integer> fileLineCounts,
        List<SkippedFile> skippedFiles) {

    /**
     * Paths of the skipped files, kept so code compiled against 1.4.0 still links.
     *
     * @return the skipped files' paths, relative to the scan root
     * @deprecated since 1.5.0, use {@link #skippedFiles()}, which also gives the reason.
     *             1.4.0 returned absolute paths; these are relative to the scan root.
     */
    @Deprecated(since = "1.5.0")
    public List<Path> unparseableFiles() {
        return skippedFiles.stream().map(skipped -> Path.of(skipped.path())).toList();
    }
}
