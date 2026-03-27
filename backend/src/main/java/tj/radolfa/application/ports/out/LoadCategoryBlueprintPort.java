package tj.radolfa.application.ports.out;

import java.util.List;

/**
 * Out-Port: read operations for category attribute blueprints.
 */
public interface LoadCategoryBlueprintPort {

    record BlueprintEntry(Long id, Long categoryId, String attributeKey, boolean required, int sortOrder) {}

    List<BlueprintEntry> findByCategoryId(Long categoryId);
}
