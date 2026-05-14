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
     * First answer: transitions PENDING → PUBLISHED.
     * Also tolerates PUBLISHED → PUBLISHED (double-click safety net).
     * Throws if the question has been rejected — a rejected question cannot
     * be resurrected via the answer flow.
     */
    public void publish(String answerText) {
        requireNonBlankAnswer(answerText);
        if (status == QuestionStatus.REJECTED) {
            throw new IllegalArgumentException(
                    "Cannot answer a rejected question: id=" + id);
        }
        this.answerText = answerText;
        this.answeredAt = Instant.now();
        this.status     = QuestionStatus.PUBLISHED;
    }

    /**
     * Edit path: replaces the answer text of an already-published question.
     * Refreshes {@code answeredAt} so "most recently answered" sorts work correctly.
     * Throws if the question is not yet published.
     */
    public void updateAnswer(String newAnswerText) {
        requireNonBlankAnswer(newAnswerText);
        if (status != QuestionStatus.PUBLISHED) {
            throw new IllegalArgumentException(
                    "Only published questions can have their answer edited: id=" + id
                            + ", status=" + status);
        }
        this.answerText = newAnswerText;
        this.answeredAt = Instant.now();
    }

    /** Rejects the question — it will never appear publicly. */
    public void reject() {
        this.status = QuestionStatus.REJECTED;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void requireNonBlankAnswer(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("answerText must not be blank");
        }
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
