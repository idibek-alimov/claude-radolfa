package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.review.RecalculateRatingSummaryUseCase;
import tj.radolfa.application.ports.out.LoadRatingSummaryPort;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.LoadReviewTraitPort;
import tj.radolfa.application.ports.out.SaveRatingSummaryPort;
import tj.radolfa.domain.model.MatchingSize;
import tj.radolfa.domain.model.Review;
import tj.radolfa.domain.model.ReviewTrait;
import tj.radolfa.domain.model.ReviewTraitInputType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RecalculateRatingSummaryService implements RecalculateRatingSummaryUseCase {

    private final LoadReviewPort        loadReviewPort;
    private final LoadReviewTraitPort   loadReviewTraitPort;
    private final SaveRatingSummaryPort saveRatingSummaryPort;

    public RecalculateRatingSummaryService(LoadReviewPort loadReviewPort,
                                           LoadReviewTraitPort loadReviewTraitPort,
                                           SaveRatingSummaryPort saveRatingSummaryPort) {
        this.loadReviewPort        = loadReviewPort;
        this.loadReviewTraitPort   = loadReviewTraitPort;
        this.saveRatingSummaryPort = saveRatingSummaryPort;
    }

    @Override
    @Transactional
    public void execute(Long listingVariantId) {
        List<Review> approved = loadReviewPort.findAllApprovedByVariant(listingVariantId);

        int reviewCount = approved.size();

        if (reviewCount == 0) {
            saveRatingSummaryPort.upsert(listingVariantId,
                    BigDecimal.ZERO, 0, zeroDistribution(), 0, 0, 0, List.of());
            return;
        }

        int ratingSum = approved.stream().mapToInt(Review::getRating).sum();
        BigDecimal averageRating = BigDecimal.valueOf(ratingSum)
                .divide(BigDecimal.valueOf(reviewCount), 2, RoundingMode.HALF_UP);

        Map<Integer, Integer> distribution = zeroDistribution();
        for (Review r : approved) {
            distribution.merge(r.getRating(), 1, Integer::sum);
        }

        int sizeAccurate   = 0;
        int sizeRunsSmall  = 0;
        int sizeRunsLarge  = 0;
        for (Review r : approved) {
            if (r.getMatchingSize() == null) continue;
            if (r.getMatchingSize() == MatchingSize.ACCURATE)        sizeAccurate++;
            else if (r.getMatchingSize() == MatchingSize.RUNS_SMALL) sizeRunsSmall++;
            else if (r.getMatchingSize() == MatchingSize.RUNS_LARGE) sizeRunsLarge++;
        }

        List<LoadRatingSummaryPort.TraitAggregateView> traitAggregates =
                computeTraitAggregates(listingVariantId, approved);

        saveRatingSummaryPort.upsert(listingVariantId,
                averageRating, reviewCount, distribution,
                sizeAccurate, sizeRunsSmall, sizeRunsLarge,
                traitAggregates);

        log.info("[RATING] Recalculated variantId={} count={} avg={} traits={}",
                listingVariantId, reviewCount, averageRating, traitAggregates.size());
    }

    private List<LoadRatingSummaryPort.TraitAggregateView> computeTraitAggregates(
            Long listingVariantId, List<Review> approved) {

        List<ReviewTrait> traits = loadReviewTraitPort.findByVariantId(listingVariantId);
        if (traits.isEmpty()) return List.of();

        // Only SLIDER traits produce a numeric average
        Map<String, ReviewTrait> sliderTraitByKey = new HashMap<>();
        for (ReviewTrait t : traits) {
            if (t.getInputType() == ReviewTraitInputType.SLIDER) {
                sliderTraitByKey.put(t.getKey(), t);
            }
        }
        if (sliderTraitByKey.isEmpty()) return List.of();

        // Accumulate: traitKey → [sum, count]
        Map<String, BigDecimal[]> totals = new HashMap<>();
        for (Review r : approved) {
            Map<String, Object> answers = r.getTraitAnswers();
            if (answers == null) continue;
            for (Map.Entry<String, Object> entry : answers.entrySet()) {
                String key = entry.getKey();
                if (!sliderTraitByKey.containsKey(key)) continue;
                BigDecimal val;
                try {
                    val = new BigDecimal(entry.getValue().toString());
                } catch (NumberFormatException ignored) {
                    continue;
                }
                totals.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                totals.get(key)[0] = totals.get(key)[0].add(val);
                totals.get(key)[1] = totals.get(key)[1].add(BigDecimal.ONE);
            }
        }

        List<LoadRatingSummaryPort.TraitAggregateView> result = new ArrayList<>();
        for (ReviewTrait trait : traits) {
            if (trait.getInputType() != ReviewTraitInputType.SLIDER) continue;
            BigDecimal[] t = totals.get(trait.getKey());
            if (t == null || t[1].compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal average = t[0].divide(t[1], 2, RoundingMode.HALF_UP);
            result.add(new LoadRatingSummaryPort.TraitAggregateView(
                    trait.getKey(),
                    trait.getLabelI18n(),
                    trait.getInputType(),
                    average,
                    t[1].intValue()
            ));
        }
        return result;
    }

    private static Map<Integer, Integer> zeroDistribution() {
        return new java.util.HashMap<>(Map.of(1, 0, 2, 0, 3, 0, 4, 0, 5, 0));
    }
}
