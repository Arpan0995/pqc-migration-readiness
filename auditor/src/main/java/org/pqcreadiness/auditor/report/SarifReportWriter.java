package org.pqcreadiness.auditor.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.Confidence;
import org.pqcreadiness.auditor.model.FileReport;
import org.pqcreadiness.auditor.model.Finding;
import org.pqcreadiness.auditor.model.ModuleReport;
import org.pqcreadiness.auditor.model.ReadinessReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serialises a {@link ReadinessReport} as SARIF 2.1.0 — the interchange format
 * GitHub code scanning ingests, so auditor findings surface as pull-request
 * annotations and Security-tab alerts in any CI pipeline.
 *
 * <p>Mapping decisions:
 * <ul>
 *   <li>One run per report; the rule array is built dynamically from the rule IDs
 *       present, with prose reused from {@link Explanations} so SARIF and Markdown
 *       reports speak with one voice.</li>
 *   <li>Scored findings map to level {@code warning}; {@link Category#INFORMATIONAL}
 *       findings and {@link Confidence#LOW} findings map to {@code note}, because LOW
 *       findings exist to surface unresolved algorithm selection, not to raise alarms
 *       (see {@link Confidence}).</li>
 *   <li>{@code security-severity} scales with the category's base difficulty weight
 *       (3 &rarr; 6.0, 2 &rarr; 4.0, 1 &rarr; 2.0), deliberately capped at GitHub's
 *       "medium" band: these are migration-effort findings under NIST IR 8547
 *       deprecation timelines, not exploitable vulnerabilities.</li>
 *   <li>File URIs are the report's root-relative paths with forward slashes, so
 *       alerts attach to files when the scanned root is the repository root.</li>
 * </ul>
 */
public final class SarifReportWriter {

    private static final String SARIF_VERSION = "2.1.0";
    private static final String SARIF_SCHEMA = "https://json.schemastore.org/sarif-2.1.0.json";
    private static final String TOOL_NAME = "pqc-readiness-auditor";
    private static final String INFO_URI = "https://github.com/Arpan0995/pqc-migration-readiness";
    private static final String RULE_HELP_URI =
            INFO_URI + "/blob/main/docs/research/02-detection-rule-catalog.md";

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String toSarif(ReadinessReport report) throws IOException {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("$schema", SARIF_SCHEMA);
        log.put("version", SARIF_VERSION);
        log.put("runs", List.of(run(report)));
        return mapper.writeValueAsString(log);
    }

    public void write(ReadinessReport report, Path out) throws IOException {
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Files.writeString(out, toSarif(report));
    }

    private Map<String, Object> run(ReadinessReport report) {
        // First occurrence of each rule ID (in report order) is its representative
        // finding; rule metadata must stand for every result the rule produces.
        Map<String, Finding> representatives = new LinkedHashMap<>();
        List<Finding> all = new ArrayList<>();
        for (ModuleReport module : report.modules()) {
            for (FileReport file : module.files()) {
                for (Finding finding : file.findings()) {
                    representatives.putIfAbsent(finding.ruleId(), finding);
                    all.add(finding);
                }
            }
        }

        List<String> ruleOrder = List.copyOf(representatives.keySet());
        List<Map<String, Object>> rules = new ArrayList<>(ruleOrder.size());
        for (String ruleId : ruleOrder) {
            rules.add(rule(ruleId, representatives.get(ruleId)));
        }

        List<Map<String, Object>> results = new ArrayList<>(all.size());
        for (Finding finding : all) {
            results.add(result(finding, ruleOrder.indexOf(finding.ruleId())));
        }

        Map<String, Object> driver = new LinkedHashMap<>();
        driver.put("name", TOOL_NAME);
        driver.put("version", report.auditorVersion());
        driver.put("informationUri", INFO_URI);
        driver.put("rules", rules);

        Map<String, Object> run = new LinkedHashMap<>();
        run.put("tool", Map.of("driver", driver));
        run.put("results", results);
        return run;
    }

    private static Map<String, Object> rule(String ruleId, Finding representative) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", ruleId);
        String description = Explanations.ruleDescription(representative);
        rule.put("shortDescription", Map.of("text", description));
        rule.put("helpUri", RULE_HELP_URI);
        rule.put("defaultConfiguration",
                Map.of("level", representative.category() == Category.INFORMATIONAL ? "note" : "warning"));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("tags", List.of("security", "cryptography", "post-quantum"));
        String severity = securitySeverity(representative.category());
        if (severity != null) {
            properties.put("security-severity", severity);
        }
        rule.put("properties", properties);
        return rule;
    }

    private static Map<String, Object> result(Finding finding, int ruleIndex) {
        Map<String, Object> region = new LinkedHashMap<>();
        region.put("startLine", Math.max(1, finding.line()));
        region.put("startColumn", Math.max(1, finding.column()));
        if (finding.snippet() != null && !finding.snippet().isBlank()) {
            region.put("snippet", Map.of("text", finding.snippet()));
        }

        Map<String, Object> physicalLocation = new LinkedHashMap<>();
        physicalLocation.put("artifactLocation",
                Map.of("uri", finding.file().replace('\\', '/')));
        physicalLocation.put("region", region);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("category", finding.category().name());
        properties.put("confidence", finding.confidence().name());
        if (!finding.fragility().isEmpty()) {
            properties.put("fragility", finding.fragility());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ruleId", finding.ruleId());
        result.put("ruleIndex", ruleIndex);
        result.put("level", level(finding));
        result.put("message", Map.of("text", Explanations.why(finding)));
        result.put("locations", List.of(Map.of("physicalLocation", physicalLocation)));
        result.put("properties", properties);
        return result;
    }

    private static String level(Finding finding) {
        if (finding.category() == Category.INFORMATIONAL
                || finding.confidence() == Confidence.LOW) {
            return "note";
        }
        return "warning";
    }

    private static String securitySeverity(Category category) {
        return switch (category.baseWeight()) {
            case 3 -> "6.0";
            case 2 -> "4.0";
            case 1 -> "2.0";
            default -> null;
        };
    }
}
