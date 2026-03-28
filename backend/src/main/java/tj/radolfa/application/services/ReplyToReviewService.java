package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.review.ReplyToReviewUseCase;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.SaveReviewPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Review;
import tj.radolfa.domain.model.ReviewStatus;

@Slf4j
@Service
public class ReplyToReviewService implements ReplyToReviewUseCase {

    private final LoadReviewPort loadReviewPort;
    private final SaveReviewPort saveReviewPort;

    public ReplyToReviewService(LoadReviewPort loadReviewPort,
                                SaveReviewPort saveReviewPort) {
        this.loadReviewPort = loadReviewPort;
        this.saveReviewPort = saveReviewPort;
    }

    @Override
    @Transactional
    public void execute(Long reviewId, String replyText) {
        Review review = loadReviewPort.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: id=" + reviewId));

        if (review.getStatus() != ReviewStatus.APPROVED) {
            throw new IllegalStateException(
                    "Cannot reply to a review that is not APPROVED (current status: " + review.getStatus() + ")");
        }

        review.postReply(replyText);
        saveReviewPort.save(review);
        log.info("[REVIEW] Seller reply posted on review id={}", reviewId);
    }
}
