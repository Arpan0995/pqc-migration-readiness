package org.pqcreadiness.auditor.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.pqcreadiness.auditor.model.ReadinessReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serialises a {@link ReadinessReport} to pretty-printed JSON — the machine-readable
 * artifact consumed by the step-4 analysis harness. The schema matches
 * {@code docs/research/03-difficulty-scoring-model.md} &sect;7.
 */
public final class JsonReportWriter {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String toJson(ReadinessReport report) throws IOException {
        return mapper.writeValueAsString(report);
    }

    public void write(ReadinessReport report, Path out) throws IOException {
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Files.writeString(out, toJson(report));
    }
}
