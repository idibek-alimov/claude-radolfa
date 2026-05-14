package tj.radolfa.application.ports.in.product;

import java.util.List;

/**
 * In-Port: reorder the images of a listing variant.
 *
 * <p>Called by MANAGER or ADMIN through the product-management API.
 */
public interface ReorderVariantImagesUseCase {

    /**
     * Persists the new image order for the given variant.
     *
     * @param variantId       the ListingVariant primary key
     * @param orderedImageIds full list of the variant's existing image IDs in the desired order;
     *                        must exactly match the set of existing image IDs
     */
    void execute(Long variantId, List<Long> orderedImageIds);
}
