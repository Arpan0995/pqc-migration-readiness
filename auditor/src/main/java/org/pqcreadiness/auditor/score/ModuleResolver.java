package org.pqcreadiness.auditor.score;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Maps a scanned source file to the module it belongs to.
 *
 * <p>A "module" is the unit the Phase 1 report is scored per, and the unit Phase 2
 * will eventually validate against measured migration effort (doc 05). We resolve
 * it by locating build descriptors ({@code pom.xml}, {@code build.gradle(.kts)}) under
 * the scan root: a file belongs to the nearest enclosing directory that has one. When
 * a codebase has no build descriptors, we fall back to the file's top-level directory,
 * which still gives a coarse but stable partition.
 */
public final class ModuleResolver {

    private static final String ROOT = "(root)";

    /** Module directories as root-relative, '/'-separated prefixes ("" == scan root). */
    private final List<String> moduleDirs;

    public ModuleResolver(Path root) {
        this.moduleDirs = discoverModuleDirs(root);
    }

    /** Resolve the module name for a root-relative source path. */
    public String resolve(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        if (!moduleDirs.isEmpty()) {
            String best = null;
            for (String dir : moduleDirs) {
                if (matches(normalized, dir) && (best == null || dir.length() > best.length())) {
                    best = dir;
                }
            }
            if (best != null) {
                return best.isEmpty() ? ROOT : best;
            }
        }
        int slash = normalized.indexOf('/');
        return slash > 0 ? normalized.substring(0, slash) : ROOT;
    }

    private static boolean matches(String path, String dir) {
        return dir.isEmpty() || path.equals(dir) || path.startsWith(dir + "/");
    }

    private static List<String> discoverModuleDirs(Path root) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<String> dirs = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(ModuleResolver::isBuildDescriptor)
                    .map(p -> root.relativize(p.getParent() == null ? p : p.getParent()))
                    .map(p -> p.toString().replace('\\', '/'))
                    .distinct()
                    .sorted(Comparator.naturalOrder())
                    .forEach(dirs::add);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to discover modules under: " + root, e);
        }
        return dirs;
    }

    private static boolean isBuildDescriptor(Path path) {
        String name = path.getFileName().toString();
        return name.equals("pom.xml") || name.equals("build.gradle") || name.equals("build.gradle.kts");
    }
}
