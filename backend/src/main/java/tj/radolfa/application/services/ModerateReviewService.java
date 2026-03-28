package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.review.ModerateReviewUseCase;
import tj.radolfa.application.ports.in.review.RecalculateRatingSummaryUseCase;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.SaveReviewPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Review;

@Slf4j
@Service
public class ModerateReviewService implements ModerateReviewUseCase {

    private final LoadReviewPort                loadReviewPort;
    private final SaveReviewPort                saveReviewPort;
    private final RecalculateRatingSummaryUseCase recalcUseCase;

    public ModerateReviewService(LoadReviewPort loadReviewPort,
                                 SaveReviewPort saveReviewPort,
                                 RecalculateRatingSummaryUseCase recalcUseCase) {
        this.loadReviewPort = loadReviewPort;
        this.saveReviewPort = saveReviewPort;
        this.recalcUseCase  = recalcUseCase;
    }

    @Override
    @Transactional
    public void approve(Long reviewId) {
        Review review = loadOrThrow(reviewId);
        review.approve();
        saveReviewPort.save(review);
        recalcUseCase.execute(review.getListingVariantId());
        log.info("[REVIEW] Approved review id={} variantId={}", reviewId, review.getListingVariantId());
    }

    @Override
    @Transactional
    public void reject(Long reviewId) {
        Review review = loadOrThrow(reviewId);
        review.reject();
        saveReviewPort.save(review);
        log.info("[REVIEW] Rejected review id={}", reviewId);
    }

    private Review loadOrThrow(Long reviewId) {
        return loadReviewPort.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: id=" + reviewId));
    }
}
