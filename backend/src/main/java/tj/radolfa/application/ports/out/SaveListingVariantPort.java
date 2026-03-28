package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ListingVariant;

import java.util.List;

public interface SaveListingVariantPort {

    void save(ListingVariant variant);

    /** Replaces all tags on the variant with the given list. Empty list removes all tags. */
    void saveTags(Long variantId, List<Long> tagIds);
}
