package tj.radolfa.application.ports.in.product;

/**
 * In-Port: create a product category natively.
 *
 * <p>Called by MANAGER or ADMIN. Categories can also arrive via the
 * import endpoint; this use case is for direct management.
 */
public interface CreateCategoryUseCase {

    /**
     * @param name     unique category name; must not be blank
     * @param parentId optional parent category ID for hierarchical categories
     * @return the ID of the newly created category
     */
    Long execute(String name, Long parentId);
}
