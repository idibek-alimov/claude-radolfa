package tj.radolfa.application.ports.out;

import java.util.Map;
import java.util.Set;

public interface ExpandCategoryTargetPort {

    /**
     * Resolves each requested root category to the set of SKU codes belonging to it
     * (or its full subtree, when includeDescendants=true). Single DB round-trip.
     *
     * @return map from each requested rootId → distinct SKU codes under that root.
     *         Roots with no matching SKUs are absent from the map.
     */
    Map<Long, Set<String>> resolveSkuCodes(Map<Long, Boolean> categoryToIncludeDescendants);
}
