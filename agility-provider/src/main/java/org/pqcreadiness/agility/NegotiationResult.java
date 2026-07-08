package org.pqcreadiness.agility;

/**
 * Outcome of a successful negotiation.
 *
 * @param suite      the selected crypto suite
 * @param downgraded true if the suite was chosen by falling back below the preferred
 *                   list (still at or above the policy floor); such events are audited
 *                   at WARN
 */
public record NegotiationResult(CryptoSuite suite, boolean downgraded) {
}
