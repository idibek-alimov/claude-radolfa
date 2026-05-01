package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ReviewTraitInputType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadRatingSummaryPort {

    Optional<RatingSummaryView> findByVariantId(Long listingVariantId);

    record RatingSummaryView(
            Long variantId,
            BigDecimal averageRating,
            int reviewCount,
            Map<Integer, Integer> distribution,
            int sizeAccurate,
            int sizeRunsSmall,
            int sizeRunsLarge,
            List<TraitAggregateView> traitAggregates
    ) {}

    record TraitAggregateView(
            String traitKey,
            String labelI18n,
            ReviewTraitInputType inputType,
            BigDecimal average,
            int count
    ) {}
}
