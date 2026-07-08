package org.pqcreadiness.agility;

/**
 * The cryptographic intent a caller wants served. Kept separate from {@link Mode}
 * because real migrations move key establishment and signatures at different speeds:
 * harvest-now-decrypt-later makes key establishment urgent first, while signatures
 * only fail once a quantum computer exists at verification time.
 */
public enum Intent {
    KEY_ESTABLISHMENT,
    SIGNATURE
}
