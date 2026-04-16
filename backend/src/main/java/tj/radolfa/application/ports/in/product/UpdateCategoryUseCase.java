package tj.radolfa.application.ports.in.product;

/**
 * In-Port: update an existing product category's name and/or parent.
 *
 * <p>Called by MANAGER or ADMIN. Slug is immutable after creation.
 * Circular parentage is rejected: a category cannot become a child of itself or its own descendants.
 */
public interface UpdateCategoryUseCase {

    /**
     * @param id       ID of the category to update
     * @param name     new unique name; must not be blank
     * @param parentId new parent category ID, or {@code null} to make it a root
     */
    void execute(Long id, String name, Long parentId);
}
