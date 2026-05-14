package tj.radolfa.application.ports.in;

import java.util.List;

/**
 * In-Port: update an existing blueprint entry for a category.
 *
 * <p>The attribute {@code type} is immutable — editing it could corrupt existing product data.
 */
public interface UpdateCategoryBlueprintUseCase {

    record Command(
            Long categoryId,
            Long blueprintId,
            String attributeKey,
            String unitName,
            List<String> allowedValues,
            boolean required,
            int sortOrder
    ) {}

    void execute(Command command);
}
