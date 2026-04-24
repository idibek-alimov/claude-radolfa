package tj.radolfa.application.ports.out;

import java.util.List;
import java.util.Map;

public interface ExpandCategoryTargetPort {

    /**
     * Resolves category targets into concrete SKU item codes in one batched query.
     * Key = categoryId, value = includeDescendants flag.
     */
    List<String> resolveSkuCodes(Map<Long, Boolean> categoryToIncludeDescendants);
}
