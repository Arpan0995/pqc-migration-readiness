package org.pqcreadiness.auditor.score;

import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.Finding;

import java.util.Map;

/**
 * Difficulty scoring model, version {@code v0}, pre-registered in
 * {@code docs/research/03-difficulty-scoring-model.md}.
 *
 * <p>The weights here are frozen <em>before</em> any migration-effort ground truth is
 * collected; the git history timestamps that freeze. Any later tuning becomes a
 * distinct {@code v1} that may only be evaluated on codebases not used for tuning.
 * This is the bias control the research design depends on, so treat these constants
 * as data, not as knobs to adjust casually.
 *
 * <p>Base category weights live on {@link Category}. Fragility multipliers are applied
 * from each finding's {@code fragility} list. Note that as of the current detection
 * waves only F4 (type coupling) is populated automatically; the remaining multipliers
 * are implemented and ready but stay latent until wave-3 dataflow detection attaches
 * their indicators to findings.
 */
public final class ScoreModel {

    public static final String VERSION = "v0";

    /** Fragility multipliers, keyed by indicator ID (doc 03 &sect;2). */
    static final Map<String, Double> FRAGILITY_MULTIPLIERS = Map.of(
            "F1", 1.5,   // fixed-size buffer adjacent
            "F2", 2.0,   // fixed-width persistence / wire format
            "F3", 1.5,   // protocol / suite pinning
            "F4", 1.5,   // concrete-type coupling on the same value
            "F6", 2.0,   // persisted key material
            "F8", 2.5,   // third-party API boundary
            "F5", 0.5);  // algorithm name config-sourced (agility credit)

    /** Cap on the product of fragility multipliers, so no single site dominates. */
    static final double MULTIPLIER_CAP = 6.0;

    /** Urgency weight for harvest-now-decrypt-later confidentiality categories. */
    static final double URGENCY_CONFIDENTIALITY = 2.0;
    /** Urgency weight for signature (authenticity) categories. */
    static final double URGENCY_SIGNATURE = 1.0;

    /** Spread coefficient: how much a module's score grows as findings spread across files. */
    static final double SPREAD_COEFFICIENT = 0.1;

    private ScoreModel() {
    }

    /**
     * Difficulty of a single finding: {@code base(category) x product(multipliers)},
     * the product capped at {@link #MULTIPLIER_CAP}. Informational findings score 0.
     */
    public static double findingDifficulty(Finding finding) {
        Category category = finding.category();
        if (!category.isScored()) {
            return 0.0;
        }
        double product = 1.0;
        for (String indicator : finding.fragility()) {
            // A type-coupling finding's own F4 is definitional, not an extra multiplier.
            if (category == Category.TYPE_COUPLING && indicator.equals("F4")) {
                continue;
            }
            product *= FRAGILITY_MULTIPLIERS.getOrDefault(indicator, 1.0);
        }
        product = Math.min(product, MULTIPLIER_CAP);
        return category.baseWeight() * product;
    }

    /** Urgency contribution of a single finding (kept orthogonal to difficulty). */
    public static double findingUrgency(Finding finding) {
        return switch (finding.category().urgencyClass()) {
            case CONFIDENTIALITY -> finding.category().baseWeight() * URGENCY_CONFIDENTIALITY;
            case SIGNATURE -> finding.category().baseWeight() * URGENCY_SIGNATURE;
            case NONE -> 0.0;
        };
    }

    /**
     * Spread factor applied to a module's summed difficulty:
     * {@code 1 + coeff * log2(1 + filesWithFindings)}. Findings concentrated in one
     * file are a focused rewrite; the same count spread across many files is a campaign.
     */
    public static double spread(int filesWithFindings) {
        return 1.0 + SPREAD_COEFFICIENT * (Math.log(1.0 + filesWithFindings) / Math.log(2.0));
    }

    /** Baseline comparator B0: the naive count of scored (vulnerable) call sites. */
    public static boolean countsTowardBaseline(Finding finding) {
        return finding.category().isScored() && finding.category() != Category.TYPE_COUPLING;
    }
}
