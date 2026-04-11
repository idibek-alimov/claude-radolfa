package tj.radolfa.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Pre-purchase Q&A question at the base-product level.
 *
 * <p>Mutable — admin can publish (with answer) or reject.
 *
 * <p>Pure Java — zero framework dependencies.
 */
public class ProductQuestion {

    private final Long           id;
    private final Long           productBaseId;
    private final Long           listingVariantId; // nullable — set when user viewed a specific variant
    private final Long           authorId;     // nullable — may be anonymous
    private final String         authorName;
    private final String         questionText;
    private       String         answerText;   // nullable until published
    private       Instant        answeredAt;   // nullable until published
    private       QuestionStatus status;
    private final Instant        createdAt;

    public ProductQuestion(Long id,
                           Long productBaseId,
                           Long listingVariantId,
                           Long authorId,
                           String authorName,
                           String questionText,
                           String answerText,
                           Instant answeredAt,
                           QuestionStatus status,
                           Instant createdAt) {

        Objects.requireNonNull(productBaseId, "productBaseId must not be null");
        Objects.requireNonNull(authorName,    "authorName must not be null");
        Objects.requireNonNull(questionText,  "questionText must not be null");

        this.id                = id;
        this.productBaseId     = productBaseId;
        this.listingVariantId  = listingVariantId;
        this.authorId          = authorId;
        this.authorName   = authorName;
        this.questionText = questionText;
        this.answerText   = answerText;
        this.answeredAt   = answeredAt;
        this.status       = status != null ? status : QuestionStatus.PENDING;
        this.createdAt    = createdAt != null ? createdAt : Instant.now();
    }

    // ── Domain mutations ──────────────────────────────────────────────────────

    /**
     * Publishes the question with an admin answer, making it visible on the storefront.
     */
    public void publish(String answerText) {
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("answerText must not be blank");
        }
        this.answerText = answerText;
        this.answeredAt = Instant.now();
        this.status     = QuestionStatus.PUBLISHED;
    }

    /** Rejects the question — it will never appear publicly. */
    public void reject() {
        this.status = QuestionStatus.REJECTED;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long           getId()              { return id; }
    public Long           getProductBaseId()   { return productBaseId; }
    public Long           getListingVariantId() { return listingVariantId; }
    public Long           getAuthorId()        { return authorId; }
    public String         getAuthorName()   { return authorName; }
    public String         getQuestionText() { return questionText; }
    public String         getAnswerText()   { return answerText; }
    public Instant        getAnsweredAt()   { return answeredAt; }
    public QuestionStatus getStatus()       { return status; }
    public Instant        getCreatedAt()    { return createdAt; }
}
