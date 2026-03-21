package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ListingVariant;

import java.util.List;
import java.util.Optional;

public interface LoadListingVariantPort {

    Optional<ListingVariant> findByProductBaseIdAndColorKey(Long productBaseId, String colorKey);

    Optional<ListingVariant> findBySlug(String slug);

    List<ListingVariant> findAllByProductBaseId(Long productBaseId);
}
