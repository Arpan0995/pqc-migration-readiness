package org.pqcreadiness.auditor.scan;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import org.pqcreadiness.auditor.model.Finding;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Walks a Java source tree and applies the detection visitors to each source file.
 *
 * <p>Scanning is source-level (JavaParser), which is what the readiness report needs:
 * readable {@code file:line} references rather than bytecode offsets. Files that fail
 * to parse are recorded and skipped, so one unparseable file never aborts a scan.
 */
public final class Scanner {

    private final ParserConfiguration configuration;

    public Scanner() {
        this.configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    /** Scan a single source root (file or directory). */
    public ScanResult scan(Path root) {
        List<Path> sources = javaSources(root);
        List<Finding> findings = new ArrayList<>();
        Map<String, Integer> lineCounts = new LinkedHashMap<>();
        List<Path> unparseable = new ArrayList<>();
        for (Path source : sources) {
            scanFile(root, source, findings, lineCounts, unparseable);
        }
        return new ScanResult(findings, lineCounts, unparseable);
    }

    private void scanFile(Path root, Path source, List<Finding> findings,
                          Map<String, Integer> lineCounts, List<Path> unparseable) {
        String relative = relativize(root, source);
        JavaParser parser = new JavaParser(configuration);
        ParseResult<CompilationUnit> result;
        try {
            result = parser.parse(source);
            lineCounts.put(relative, nonBlankLines(source));
        } catch (IOException e) {
            unparseable.add(source);
            return;
        }
        if (result.getResult().isEmpty() || !result.isSuccessful()) {
            unparseable.add(source);
            return;
        }
        ScanContext ctx = new ScanContext(relative, findings);
        result.getResult().get().accept(new CryptoUsageVisitor(), ctx);
    }

    private static int nonBlankLines(Path source) throws IOException {
        try (Stream<String> lines = Files.lines(source)) {
            return (int) lines.filter(line -> !line.isBlank()).count();
        }
    }

    private static List<Path> javaSources(Path root) {
        if (Files.isRegularFile(root)) {
            return isJava(root) ? List.of(root) : List.of();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(Scanner::isJava)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk source root: " + root, e);
        }
    }

    private static boolean isJava(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".java") && !name.equals("module-info.java")
                && !name.equals("package-info.java");
    }

    private static String relativize(Path root, Path source) {
        Path base = Files.isRegularFile(root) ? root.getParent() : root;
        Path relative = (base == null ? source : base.relativize(source));
        return relative.toString().replace('\\', '/');
    }
}
