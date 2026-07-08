package org.pqcreadiness.agility;

/**
 * Thrown when no acceptable suite can be negotiated: either the policy is fail-closed
 * and there is no intersection, or a downgrade found nothing meeting the floor.
 */
public class NegotiationException extends RuntimeException {

    public NegotiationException(String message) {
        super(message);
    }
}
