package org.pqcreadiness.auditor.scan;

/**
 * Recognises conventional test source roots by path.
 *
 * <p>Test code routinely exercises quantum-vulnerable algorithms deliberately, so it
 * inflates a naive finding count without representing production migration surface. The
 * auditor therefore classifies findings rather than silently dropping them: totals stay
 * comparable across runs, and the reports separate production from test.
 *
 * <p>Detection is by path only, matching the Maven and Gradle conventions
 * ({@code src/test/java}, {@code src/testFixtures/java}, {@code src/integrationTest/java},
 * {@code src/androidTest/java}). A directory merely named {@code test} lower in a main
 * source tree does not count, since that would misclassify production packages such as
 * {@code org/example/test/}.
 */
public final class TestSources {

    private TestSources() {
    }

    /**
     * Whether a root-relative path sits under a conventional test source root.
     *
     * @param relativePath path relative to the scanned root, forward-slash separated
     */
    public static boolean isTestSourcePath(String relativePath) {
        if (relativePath == null) {
            return false;
        }
        String path = "/" + relativePath.replace('\\', '/');
        return path.contains("/src/test/")
                || path.contains("/src/testFixtures/")
                || path.contains("/src/integrationTest/")
                || path.contains("/src/androidTest/")
                || path.contains("/src/it/");
    }
}
