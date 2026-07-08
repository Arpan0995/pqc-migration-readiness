package org.pqcreadiness.agility;

import java.util.List;
import java.util.Map;

/**
 * What a party can do: for each intent, the ordered list of suite IDs it supports
 * (most preferred first). Mirrors TLS named-group / SSH kex-list negotiation so the
 * model is familiar and analysable. The transport of these descriptors between peers
 * is the caller's concern; this layer only performs in-process selection.
 *
 * @param offeredSuiteIds intent -&gt; ordered suite IDs the party supports
 */
public record CapabilityDescriptor(Map<Intent, List<String>> offeredSuiteIds) {

    public CapabilityDescriptor {
        offeredSuiteIds = Map.copyOf(offeredSuiteIds);
    }

    public List<String> offered(Intent intent) {
        return offeredSuiteIds.getOrDefault(intent, List.of());
    }

    public boolean supports(Intent intent, String suiteId) {
        return offered(intent).contains(suiteId);
    }
}
