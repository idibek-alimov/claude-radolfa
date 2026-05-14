package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.AttributeType;

import java.util.List;

/**
 * Out-Port: read operations for category attribute blueprints.
 */
public interface LoadCategoryBlueprintPort {

    record BlueprintEntry(
            Long id,
            Long categoryId,
            String attributeKey,
            AttributeType type,
            String unitName,
            List<String> allowedValues,
            boolean required,
            int sortOrder
    ) {}

    List<BlueprintEntry> findByCategoryId(Long categoryId);
}
