package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.AwardReviewBonusUseCase;
import tj.radolfa.application.ports.in.review.ModerateReviewUseCase;
import tj.radolfa.application.ports.in.review.RecalculateRatingSummaryUseCase;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveReviewPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Review;
import tj.radolfa.domain.model.ReviewStatus;

import java.time.Instant;

@Slf4j
@Service
public class ModerateReviewService implements ModerateReviewUseCase {

    private final LoadReviewPort                  loadReviewPort;
    private final SaveReviewPort                  saveReviewPort;
    private final RecalculateRatingSummaryUseCase recalcUseCase;
    private final AwardReviewBonusUseCase         awardReviewBonusUseCase;
    private final NotificationPort                notificationPort;

    public ModerateReviewService(LoadReviewPort loadReviewPort,
                                 SaveReviewPort saveReviewPort,
                                 RecalculateRatingSummaryUseCase recalcUseCase,
                                 AwardReviewBonusUseCase awardReviewBonusUseCase,
                                 NotificationPort notificationPort) {
        this.loadReviewPort         = loadReviewPort;
        this.saveReviewPort         = saveReviewPort;
        this.recalcUseCase          = recalcUseCase;
        this.awardReviewBonusUseCase = awardReviewBonusUseCase;
        this.notificationPort       = notificationPort;
    }

    @Override
    @Transactional
    public void approve(Long reviewId) {
        Review review = loadOrThrow(reviewId);

        // Idempotency — already approved: skip all side effects
        if (review.getStatus() == ReviewStatus.APPROVED) {
            log.info("[REVIEW] approve called on already-approved review id={} — skipping", reviewId);
            return;
        }

        review.approve();

        if (review.getPointsAwardedAt() == null) {
            review.markPointsAwarded(Instant.now());
        }

        saveReviewPort.save(review);
        recalcUseCase.execute(review.getListingVariantId());

        awardReviewBonusUseCase.execute(review.getAuthorId());
        notificationPort.sendReviewApprovedNotification(review.getAuthorId(), review.getId());

        log.info("[REVIEW] Approved review id={} variantId={} authorId={}",
                reviewId, review.getListingVariantId(), review.getAuthorId());
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
