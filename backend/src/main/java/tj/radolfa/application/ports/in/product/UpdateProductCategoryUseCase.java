package tj.radolfa.application.ports.in.product;

/**
 * In-Port: reassign the category of a ProductBase.
 *
 * <p>Called by MANAGER or ADMIN. Resolves categoryId → category name,
 * updates the ProductBase, and re-indexes all listing variants in Elasticsearch.
 */
public interface UpdateProductCategoryUseCase {

    /** @param productBaseId the ID of the ProductBase to update
     *  @param categoryId   the ID of the target category */
    void execute(Long productBaseId, Long categoryId);
}
