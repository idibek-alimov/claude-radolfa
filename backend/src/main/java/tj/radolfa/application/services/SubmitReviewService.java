package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.review.SubmitReviewUseCase;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.LoadReviewTraitPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveReviewPort;
import tj.radolfa.application.ports.out.VerifyPurchasePort;
import tj.radolfa.domain.exception.DuplicateReviewException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.exception.UnauthorizedReviewException;
import tj.radolfa.domain.model.Review;
import tj.radolfa.domain.model.ReviewStatus;
import tj.radolfa.domain.model.ReviewTrait;
import tj.radolfa.domain.model.ReviewTraitInputType;
import tj.radolfa.domain.model.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SubmitReviewService implements SubmitReviewUseCase {

    private final LoadUserPort       loadUserPort;
    private final VerifyPurchasePort verifyPurchasePort;
    private final LoadReviewPort     loadReviewPort;
    private final SaveReviewPort     saveReviewPort;
    private final LoadReviewTraitPort loadReviewTraitPort;

    public SubmitReviewService(LoadUserPort loadUserPort,
                               VerifyPurchasePort verifyPurchasePort,
                               LoadReviewPort loadReviewPort,
                               SaveReviewPort saveReviewPort,
                               LoadReviewTraitPort loadReviewTraitPort) {
        this.loadUserPort        = loadUserPort;
        this.verifyPurchasePort  = verifyPurchasePort;
        this.loadReviewPort      = loadReviewPort;
        this.saveReviewPort      = saveReviewPort;
        this.loadReviewTraitPort = loadReviewTraitPort;
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

        // 4. Validate and sanitize trait answers
        Map<String, Object> resolvedTraitAnswers = validateTraitAnswers(
                command.traitAnswers(), command.listingVariantId());

        // 5. Build domain object
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
                null,
                resolvedTraitAnswers
        );

        Review saved = saveReviewPort.save(review);
        log.info("[REVIEW] Submitted review id={} variantId={} authorId={} rating={}",
                saved.getId(), command.listingVariantId(), command.authorId(), command.rating());

        return saved.getId();
    }

    private Map<String, Object> validateTraitAnswers(
            Map<String, Object> raw, Long listingVariantId) {

        if (raw == null || raw.isEmpty()) return null;

        List<ReviewTrait> validTraits = loadReviewTraitPort.findByVariantId(listingVariantId);
        Map<String, ReviewTrait> traitByKey = validTraits.stream()
                .collect(Collectors.toMap(ReviewTrait::getKey, t -> t));

        Map<String, Object> validated = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String key   = entry.getKey();
            Object value = entry.getValue();

            ReviewTrait trait = traitByKey.get(key);
            if (trait == null) {
                throw new IllegalArgumentException(
                        "Unknown review trait key for this product: '" + key + "'");
            }

            if (trait.getInputType() == ReviewTraitInputType.SLIDER) {
                int intValue;
                try {
                    intValue = ((Number) value).intValue();
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException(
                            "SLIDER trait '" + key + "' value must be a number, got: " + value);
                }
                if (intValue < 1 || intValue > 5) {
                    throw new IllegalArgumentException(
                            "SLIDER trait '" + key + "' value must be between 1 and 5, got: " + intValue);
                }
                validated.put(key, intValue);
            } else {
                // RADIO — accept any non-blank string (options not enforced until options table exists)
                String strValue = value == null ? "" : value.toString().trim();
                if (strValue.isBlank()) {
                    throw new IllegalArgumentException(
                            "RADIO trait '" + key + "' value must not be blank");
                }
                if (strValue.length() > 64) {
                    throw new IllegalArgumentException(
                            "RADIO trait '" + key + "' value exceeds 64 characters");
                }
                validated.put(key, strValue);
            }
        }
        return Map.copyOf(validated);
    }
}
