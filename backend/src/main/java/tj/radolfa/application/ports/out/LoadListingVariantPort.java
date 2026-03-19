package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ListingVariant;

import java.util.Optional;

public interface LoadListingVariantPort {

    Optional<ListingVariant> findVariantById(Long id);

    Optional<ListingVariant> findByProductBaseIdAndColorKey(Long productBaseId, String colorKey);

    Optional<ListingVariant> findBySlug(String slug);
}
