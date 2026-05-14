package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.AttributeType;

import java.util.List;

/**
 * In-Port: creates a new attribute blueprint entry for a category.
 */
public interface CreateCategoryBlueprintUseCase {

    record Command(
            Long categoryId,
            String attributeKey,
            AttributeType type,
            String unitName,
            List<String> allowedValues,
            boolean required,
            int sortOrder
    ) {}

    /**
     * @return the ID of the newly created blueprint entry
     * @throws tj.radolfa.domain.exception.ResourceNotFoundException if the category does not exist
     */
    Long execute(Command command);
}
