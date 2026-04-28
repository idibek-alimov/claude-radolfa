package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.review.SubmitReviewUseCase;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveReviewPort;
import tj.radolfa.application.ports.out.VerifyPurchasePort;
import tj.radolfa.domain.exception.DuplicateReviewException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.exception.UnauthorizedReviewException;
import tj.radolfa.domain.model.Review;
import tj.radolfa.domain.model.ReviewStatus;
import tj.radolfa.domain.model.User;

import java.util.List;

@Slf4j
@Service
public class SubmitReviewService implements SubmitReviewUseCase {

    private final LoadUserPort      loadUserPort;
    private final VerifyPurchasePort verifyPurchasePort;
    private final LoadReviewPort    loadReviewPort;
    private final SaveReviewPort    saveReviewPort;

    public SubmitReviewService(LoadUserPort loadUserPort,
                               VerifyPurchasePort verifyPurchasePort,
                               LoadReviewPort loadReviewPort,
                               SaveReviewPort saveReviewPort) {
        this.loadUserPort       = loadUserPort;
        this.verifyPurchasePort = verifyPurchasePort;
        this.loadReviewPort     = loadReviewPort;
        this.saveReviewPort     = saveReviewPort;
    }

    @Override
    @Transactional
    public Long execute(Command command) {
        // 1. Resolve author name server-side — never trust the client
        User author = loadUserPort.loadById(command.authorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: id=" + command.authorId()));

        String authorName = (author.name() != null && !author.name().isBlank())
                ? author.name()
                : "Anonymous";

        // 2. Verified purchase check
        if (!verifyPurchasePort.hasPurchasedVariant(command.authorId(), command.listingVariantId())) {
            throw new UnauthorizedReviewException(
                    "You can only review products you have purchased.");
        }

        // 3. Duplicate check
        if (loadReviewPort.existsByOrderAndVariant(command.orderId(), command.listingVariantId())) {
            throw new DuplicateReviewException(
                    "You have already submitted a review for this product.");
        }

        // 4. Build domain object
        Review review = new Review(
                null,
                command.listingVariantId(),
                command.skuId(),
                command.orderId(),
                command.authorId(),
                authorName,
                command.rating(),
                command.title(),
                command.body(),
                command.pros(),
                command.cons(),
                command.matchingSize(),
                command.photoUrls() != null ? List.copyOf(command.photoUrls()) : List.of(),
                ReviewStatus.PENDING,
                null,
                null,
                null,
                null
        );

        Review saved = saveReviewPort.save(review);
        log.info("[REVIEW] Submitted review id={} variantId={} authorId={} rating={}",
                saved.getId(), command.listingVariantId(), command.authorId(), command.rating());

        return saved.getId();
    }
}
