package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ListingVariant;

import java.util.List;

public interface SaveListingVariantPort {

    void save(ListingVariant variant);

    /** Replaces all tags on the variant with the given list. Empty list removes all tags. */
    void saveTags(Long variantId, List<Long> tagIds);

    /**
     * Updates {@code sort_order} and {@code is_primary} flags for the given variant's images
     * in place. {@code orderedImageIds} must exactly match the set of existing image IDs for
     * the variant; throws {@link IllegalArgumentException} otherwise.
     */
    void reorderImages(Long variantId, List<Long> orderedImageIds);
}
