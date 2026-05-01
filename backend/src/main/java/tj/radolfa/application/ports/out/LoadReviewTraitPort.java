package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ReviewTrait;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadReviewTraitPort {

    Optional<ReviewTrait> findById(Long id);

    Optional<ReviewTrait> findByKey(String key);

    List<ReviewTrait> findAll();

    List<ReviewTrait> findAllByIds(Collection<Long> ids);

    boolean existsByKey(String key);

    /**
     * Returns all traits applicable to the given listing variant, resolved
     * by walking the product's category parent chain and collecting the union
     * of traits from every ancestor (including the leaf category itself).
     * Results are deduplicated by trait id.
     */
    List<ReviewTrait> findByVariantId(Long listingVariantId);
}
