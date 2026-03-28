package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.review.RecalculateRatingSummaryUseCase;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.SaveRatingSummaryPort;
import tj.radolfa.domain.model.MatchingSize;
import tj.radolfa.domain.model.Review;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RecalculateRatingSummaryService implements RecalculateRatingSummaryUseCase {

    private final LoadReviewPort        loadReviewPort;
    private final SaveRatingSummaryPort saveRatingSummaryPort;

    public RecalculateRatingSummaryService(LoadReviewPort loadReviewPort,
                                           SaveRatingSummaryPort saveRatingSummaryPort) {
        this.loadReviewPort        = loadReviewPort;
        this.saveRatingSummaryPort = saveRatingSummaryPort;
    }

    @Override
    @Transactional
    public void execute(Long listingVariantId) {
        List<Review> approved = loadReviewPort.findAllApprovedByVariant(listingVariantId);

        int reviewCount = approved.size();

        if (reviewCount == 0) {
            saveRatingSummaryPort.upsert(listingVariantId,
                    BigDecimal.ZERO, 0, zeroDistribution(), 0, 0, 0);
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
            if (r.getMatchingSize() == MatchingSize.ACCURATE)    sizeAccurate++;
            else if (r.getMatchingSize() == MatchingSize.RUNS_SMALL) sizeRunsSmall++;
            else if (r.getMatchingSize() == MatchingSize.RUNS_LARGE) sizeRunsLarge++;
        }

        saveRatingSummaryPort.upsert(listingVariantId,
                averageRating, reviewCount, distribution,
                sizeAccurate, sizeRunsSmall, sizeRunsLarge);

        log.info("[RATING] Recalculated variantId={} count={} avg={}",
                listingVariantId, reviewCount, averageRating);
    }

    private static Map<Integer, Integer> zeroDistribution() {
        return new java.util.HashMap<>(Map.of(1, 0, 2, 0, 3, 0, 4, 0, 5, 0));
    }
}
