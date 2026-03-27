package tj.radolfa.application.ports.in;

import java.util.List;

/**
 * In-Port: returns the attribute blueprint for a given category.
 * The blueprint tells the frontend which attributes to surface and which are required.
 */
public interface GetCategoryBlueprintUseCase {

    record BlueprintEntryDto(String attributeKey, boolean required, int sortOrder) {}

    /**
     * @param categoryId the category ID
     * @return ordered list of blueprint entries; empty if no blueprint is configured
     * @throws IllegalArgumentException if the category does not exist
     */
    List<BlueprintEntryDto> getBlueprint(Long categoryId);
}
