package org.pqcreadiness.auditor.scan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Path classification for conventional test source roots. */
class TestSourcesTest {

    @Test
    void recognisesConventionalTestRoots() {
        assertTrue(TestSources.isTestSourcePath("src/test/java/a/B.java"));
        assertTrue(TestSources.isTestSourcePath("mod/src/test/java/a/B.java"));
        assertTrue(TestSources.isTestSourcePath("src/testFixtures/java/a/B.java"));
        assertTrue(TestSources.isTestSourcePath("src/integrationTest/java/a/B.java"));
        assertTrue(TestSources.isTestSourcePath("src/androidTest/java/a/B.java"));
        assertTrue(TestSources.isTestSourcePath("src/it/java/a/B.java"));
        // Deep nesting, as in Keycloak's tests/base module.
        assertTrue(TestSources.isTestSourcePath("tests/base/src/test/java/a/B.java"));
    }

    @Test
    void doesNotMisclassifyProductionCode() {
        assertFalse(TestSources.isTestSourcePath("src/main/java/a/B.java"));
        // A production package merely named "test" must not count.
        assertFalse(TestSources.isTestSourcePath("src/main/java/org/example/test/Helper.java"));
        // A module named "testing" is not a test source root by itself.
        assertFalse(TestSources.isTestSourcePath("testing/src/main/java/a/B.java"));
        assertFalse(TestSources.isTestSourcePath(null));
    }

    @Test
    void normalisesWindowsSeparators() {
        assertTrue(TestSources.isTestSourcePath("mod\\src\\test\\java\\a\\B.java"));
    }
}
