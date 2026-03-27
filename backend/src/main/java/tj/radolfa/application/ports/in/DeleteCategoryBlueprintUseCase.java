package tj.radolfa.application.ports.in;

/**
 * In-Port: deletes a blueprint entry from a category.
 */
public interface DeleteCategoryBlueprintUseCase {

    /**
     * @param categoryId  the owning category ID (validates the blueprint belongs to it)
     * @param blueprintId the blueprint entry ID to delete
     * @throws tj.radolfa.domain.exception.ResourceNotFoundException if the blueprint is not found for that category
     */
    void execute(Long categoryId, Long blueprintId);
}
