package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.AttributeType;

import java.util.List;

/**
 * In-Port: returns the attribute blueprint for a given category.
 * The blueprint tells the frontend which attributes to surface and which are required.
 */
public interface GetCategoryBlueprintUseCase {

    record BlueprintEntryDto(
            Long id,
            String attributeKey,
            AttributeType type,
            String unitName,
            List<String> allowedValues,
            boolean required,
            int sortOrder
    ) {}

    /**
     * @param categoryId the category ID
     * @return ordered list of blueprint entries; empty if no blueprint is configured
     * @throws tj.radolfa.domain.exception.ResourceNotFoundException if the category does not exist
     */
    List<BlueprintEntryDto> getBlueprint(Long categoryId);
}
