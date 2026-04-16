package tj.radolfa.application.ports.out;

import tj.radolfa.application.ports.in.UpdateCategoryBlueprintUseCase;
import tj.radolfa.domain.model.AttributeType;

import java.util.List;

/**
 * Out-Port: write operations for category attribute blueprints.
 */
public interface SaveCategoryBlueprintPort {

    Long save(Long categoryId, String attributeKey, AttributeType type, String unitName,
              List<String> allowedValues, boolean required, int sortOrder);

    void update(UpdateCategoryBlueprintUseCase.Command command);

    void deleteById(Long blueprintId);
}
