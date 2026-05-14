package tj.radolfa.application.ports.in.product;

/**
 * In-Port: delete a product category.
 *
 * <p>ADMIN only. Throws if the category is still assigned to any product.
 */
public interface DeleteCategoryUseCase {

    /** @param categoryId the ID of the category to delete */
    void execute(Long categoryId);
}
