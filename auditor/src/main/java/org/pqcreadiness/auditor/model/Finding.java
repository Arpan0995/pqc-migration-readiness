package org.pqcreadiness.auditor.model;

import java.util.List;

/**
 * A single flagged crypto usage site.
 *
 * <p>Findings are the atomic unit of the auditor's output. They join to
 * hand-labeled ground truth on {@code ruleId} + {@code file} + {@code line}
 * during precision/recall evaluation, and they are aggregated into file and
 * module scores by the scoring engine.
 *
 * @param ruleId     machine-readable rule identifier, e.g. {@code JCA-KPG-RSA}
 * @param file       path to the source file, relative to the scanned root
 * @param line       1-based line of the flagged expression
 * @param column     1-based column of the flagged expression
 * @param api        the JCA/library entry point, e.g. {@code KeyPairGenerator}
 * @param algorithm  the resolved algorithm token, e.g. {@code RSA}
 * @param category   functional category, carrying the base difficulty weight
 * @param confidence how certain the resolution is
 * @param fragility  fragility indicator IDs co-located with this finding (F1..F8);
 *                   empty in wave 1, populated once fragility detection lands
 * @param snippet    the source text of the flagged expression, for the report
 */
public record Finding(
        String ruleId,
        String file,
        int line,
        int column,
        String api,
        String algorithm,
        Category category,
        Confidence confidence,
        List<String> fragility,
        String snippet) {

    public Finding {
        fragility = fragility == null ? List.of() : List.copyOf(fragility);
    }
}
