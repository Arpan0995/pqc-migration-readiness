package org.pqcreadiness.auditor.model;

/**
 * A source file that was skipped during scanning, with its root-relative path
 * and the reason it could not be analyzed.
 *
 * @param path   source path relative to the scan root
 * @param reason explanation of why the file was skipped
 */
public record SkippedFile(String path, String reason) {
}