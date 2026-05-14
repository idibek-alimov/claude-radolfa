package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ListingVariant;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadListingVariantPort {

    Optional<ListingVariant> findVariantById(Long id);

    Optional<ListingVariant> findByProductBaseIdAndColorKey(Long productBaseId, String colorKey);

    Optional<ListingVariant> findBySlug(String slug);

    List<ListingVariant> findAllByProductBaseId(Long productBaseId);

    Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids);
}
