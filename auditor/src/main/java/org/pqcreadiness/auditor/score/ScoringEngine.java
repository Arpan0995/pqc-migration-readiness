package org.pqcreadiness.auditor.score;

import org.pqcreadiness.auditor.model.FileReport;
import org.pqcreadiness.auditor.model.Finding;
import org.pqcreadiness.auditor.model.ModuleReport;
import org.pqcreadiness.auditor.model.ReadinessReport;
import org.pqcreadiness.auditor.scan.ScanResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates raw findings into a {@link ReadinessReport} using score model v0
 * (see {@code docs/research/03-difficulty-scoring-model.md}).
 *
 * <p>Detection and scoring are deliberately decoupled: this engine consumes findings
 * and knows nothing about how they were produced, so the scoring methodology — the
 * research contribution — can be tested and evolved independently of the scanner.
 */
public final class ScoringEngine {

    private final String auditorVersion;

    public ScoringEngine(String auditorVersion) {
        this.auditorVersion = auditorVersion;
    }

    public ReadinessReport score(String codebase, ScanResult scan, ModuleResolver resolver) {
        Map<String, List<Finding>> findingsByModule = new LinkedHashMap<>();
        for (Finding finding : scan.findings()) {
            findingsByModule.computeIfAbsent(resolver.resolve(finding.file()), k -> new ArrayList<>())
                    .add(finding);
        }

        Map<String, Integer> locByModule = new LinkedHashMap<>();
        scan.fileLineCounts().forEach((path, loc) ->
                locByModule.merge(resolver.resolve(path), loc, Integer::sum));

        List<ModuleReport> modules = new ArrayList<>();
        findingsByModule.forEach((module, findings) ->
                modules.add(buildModule(module, findings, locByModule.getOrDefault(module, 0))));
        modules.sort(Comparator.comparingDouble(ModuleReport::score).reversed()
                .thenComparing(ModuleReport::name));

        return new ReadinessReport(
                codebase,
                auditorVersion,
                ScoreModel.VERSION,
                Instant.now().toString(),
                scan.findings().size(),
                scan.fileLineCounts().size(),
                scan.unparseableFiles().size(),
                modules);
    }

    private ModuleReport buildModule(String module, List<Finding> findings, int loc) {
        Map<String, List<Finding>> byFile = new LinkedHashMap<>();
        for (Finding finding : findings) {
            byFile.computeIfAbsent(finding.file(), k -> new ArrayList<>()).add(finding);
        }

        List<FileReport> files = new ArrayList<>();
        double summedDifficulty = 0.0;
        double urgency = 0.0;
        int baselineCount = 0;
        for (Map.Entry<String, List<Finding>> entry : byFile.entrySet()) {
            double fileScore = 0.0;
            for (Finding finding : entry.getValue()) {
                fileScore += ScoreModel.findingDifficulty(finding);
                urgency += ScoreModel.findingUrgency(finding);
                if (ScoreModel.countsTowardBaseline(finding)) {
                    baselineCount++;
                }
            }
            summedDifficulty += fileScore;
            files.add(new FileReport(entry.getKey(), round(fileScore), entry.getValue()));
        }
        files.sort(Comparator.comparingDouble(FileReport::score).reversed()
                .thenComparing(FileReport::path));

        double score = summedDifficulty * ScoreModel.spread(byFile.size());
        return new ModuleReport(module, loc, round(score), round(urgency), baselineCount, files);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
