package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadRatingSummaryPort;
import tj.radolfa.application.ports.out.SaveRatingSummaryPort;
import tj.radolfa.infrastructure.persistence.entity.ProductRatingSummaryEntity;
import tj.radolfa.infrastructure.persistence.repository.ProductRatingSummaryRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
public class RatingSummaryAdapter implements LoadRatingSummaryPort, SaveRatingSummaryPort {

    private final ProductRatingSummaryRepository repository;

    public RatingSummaryAdapter(ProductRatingSummaryRepository repository) {
        this.repository = repository;
    }

    // ---- LoadRatingSummaryPort -----------------------------------------

    @Override
    public Optional<RatingSummaryView> findByVariantId(Long listingVariantId) {
        return repository.findById(listingVariantId).map(this::toView);
    }

    // ---- SaveRatingSummaryPort -----------------------------------------

    @Override
    public void upsert(Long listingVariantId,
                       BigDecimal averageRating,
                       int reviewCount,
                       Map<Integer, Integer> distribution,
                       int sizeAccurate,
                       int sizeRunsSmall,
                       int sizeRunsLarge) {

        ProductRatingSummaryEntity entity = repository.findById(listingVariantId)
                .orElseGet(() -> {
                    ProductRatingSummaryEntity e = new ProductRatingSummaryEntity();
                    e.setListingVariantId(listingVariantId);
                    return e;
                });

        entity.setAverageRating(averageRating);
        entity.setReviewCount(reviewCount);
        entity.setCount5(distribution.getOrDefault(5, 0));
        entity.setCount4(distribution.getOrDefault(4, 0));
        entity.setCount3(distribution.getOrDefault(3, 0));
        entity.setCount2(distribution.getOrDefault(2, 0));
        entity.setCount1(distribution.getOrDefault(1, 0));
        entity.setSizeAccurate(sizeAccurate);
        entity.setSizeRunsSmall(sizeRunsSmall);
        entity.setSizeRunsLarge(sizeRunsLarge);
        entity.setLastCalculatedAt(Instant.now());

        repository.saveAndFlush(entity);
    }

    // ---- Private -------------------------------------------------------

    private RatingSummaryView toView(ProductRatingSummaryEntity e) {
        Map<Integer, Integer> distribution = Map.of(
                5, e.getCount5(),
                4, e.getCount4(),
                3, e.getCount3(),
                2, e.getCount2(),
                1, e.getCount1()
        );
        return new RatingSummaryView(
                e.getListingVariantId(),
                e.getAverageRating(),
                e.getReviewCount(),
                distribution,
                e.getSizeAccurate(),
                e.getSizeRunsSmall(),
                e.getSizeRunsLarge()
        );
    }
}
