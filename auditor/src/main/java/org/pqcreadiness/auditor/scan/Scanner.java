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
import java.util.Set;
import java.util.TreeSet;
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

    /**
     * Scan a single source root (file or directory) in two passes: parse every file and
     * build the project-wide constant table, then run detection with that table
     * available so cross-file algorithm constants ({@code Type.FIELD}) resolve.
     */
    public ScanResult scan(Path root) {
        List<Path> sources = javaSources(root);
        Map<String, Integer> lineCounts = new LinkedHashMap<>();
        List<Path> unparseable = new ArrayList<>();

        // Pass 1: parse all files, keeping the successfully parsed units.
        List<ParsedFile> parsed = new ArrayList<>();
        for (Path source : sources) {
            parseFile(root, source, parsed, lineCounts, unparseable);
        }
        ConstantIndex constants = ConstantIndex.build(
                parsed.stream().map(ParsedFile::unit).toList());

        // Pass 2: detect, resolving arguments against the project-wide constant table.
        List<Finding> findings = new ArrayList<>();
        for (ParsedFile file : parsed) {
            visitFile(file, constants, findings);
        }
        return new ScanResult(findings, lineCounts, unparseable);
    }

    private record ParsedFile(String relativePath, CompilationUnit unit) {
    }

    private void parseFile(Path root, Path source, List<ParsedFile> parsed,
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
        parsed.add(new ParsedFile(relative, result.getResult().get()));
    }

    private void visitFile(ParsedFile file, ConstantIndex constants, List<Finding> findings) {
        List<ScanContext.ScopedFinding> scoped = new ArrayList<>();
        List<ScanContext.Signal> signals = new ArrayList<>();
        ScanContext ctx = new ScanContext(file.relativePath(), scoped, signals, constants);
        file.unit().accept(new DetectionVisitor(), ctx);
        merge(scoped, signals, findings);
    }

    /**
     * Attach fragility indicators to the crypto findings they qualify: a finding
     * takes every indicator signalled within its enclosing scope, provided its
     * category accepts fragility tags (primary crypto usages, not structural findings).
     */
    private static void merge(List<ScanContext.ScopedFinding> scoped,
                              List<ScanContext.Signal> signals, List<Finding> out) {
        Map<String, Set<String>> byScope = new LinkedHashMap<>();
        for (ScanContext.Signal signal : signals) {
            byScope.computeIfAbsent(signal.scopeKey(), k -> new TreeSet<>()).add(signal.indicator());
        }
        for (ScanContext.ScopedFinding sf : scoped) {
            Finding finding = sf.finding();
            Set<String> inScope = byScope.get(sf.scopeKey());
            if (finding.category().acceptsFragilityTags() && inScope != null && !inScope.isEmpty()) {
                Set<String> merged = new TreeSet<>(finding.fragility());
                merged.addAll(inScope);
                out.add(finding.withFragility(List.copyOf(merged)));
            } else {
                out.add(finding);
            }
        }
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
