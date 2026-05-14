package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadRatingSummaryPort;
import tj.radolfa.application.ports.out.SaveRatingSummaryPort;
import tj.radolfa.domain.model.ReviewTraitInputType;
import tj.radolfa.infrastructure.persistence.entity.ProductRatingSummaryEntity;
import tj.radolfa.infrastructure.persistence.repository.ProductRatingSummaryRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
                       int sizeRunsLarge,
                       List<TraitAggregateView> traitAggregates) {

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
        entity.setTraitAggregates(toRawMaps(traitAggregates));
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
                e.getSizeRunsLarge(),
                fromRawMaps(e.getTraitAggregates())
        );
    }

    private List<TraitAggregateView> fromRawMaps(List<Map<String, Object>> raw) {
        if (raw == null) return List.of();
        List<TraitAggregateView> result = new ArrayList<>(raw.size());
        for (Map<String, Object> m : raw) {
            try {
                result.add(new TraitAggregateView(
                        (String) m.get("traitKey"),
                        (String) m.get("labelI18n"),
                        ReviewTraitInputType.valueOf((String) m.get("inputType")),
                        new BigDecimal(m.get("average").toString()),
                        ((Number) m.get("count")).intValue()
                ));
            } catch (Exception ignored) {
                // Skip malformed entries rather than failing a read
            }
        }
        return result;
    }

    private List<Map<String, Object>> toRawMaps(List<TraitAggregateView> views) {
        if (views == null) return List.of();
        List<Map<String, Object>> result = new ArrayList<>(views.size());
        for (TraitAggregateView v : views) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("traitKey",   v.traitKey());
            m.put("labelI18n",  v.labelI18n());
            m.put("inputType",  v.inputType().name());
            m.put("average",    v.average());
            m.put("count",      v.count());
            result.add(m);
        }
        return result;
    }
}
