package tj.radolfa.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Customer review aggregate.
 *
 * <p>Mutable — status transitions and seller replies are applied via named domain methods.
 *
 * <p>Pure Java — zero framework dependencies.
 */
public class Review {

    private final Long          id;
    private final Long          listingVariantId;
    private final Long          skuId;           // nullable — which size was reviewed
    private final Long          orderId;
    private final Long          authorId;
    private final String        authorName;
    private final int           rating;
    private final String        title;           // nullable
    private final String        body;
    private final String        pros;            // nullable
    private final String        cons;            // nullable
    private final MatchingSize  matchingSize;    // nullable
    private final List<String>  photos;
    private       ReviewStatus  status;
    private       String        sellerReply;     // nullable
    private       Instant       sellerRepliedAt; // nullable
    private final Instant       createdAt;
    private       Instant       updatedAt;

    public Review(Long id,
                  Long listingVariantId,
                  Long skuId,
                  Long orderId,
                  Long authorId,
                  String authorName,
                  int rating,
                  String title,
                  String body,
                  String pros,
                  String cons,
                  MatchingSize matchingSize,
                  List<String> photos,
                  ReviewStatus status,
                  String sellerReply,
                  Instant sellerRepliedAt,
                  Instant createdAt,
                  Instant updatedAt) {

        Objects.requireNonNull(listingVariantId, "listingVariantId must not be null");
        Objects.requireNonNull(orderId,          "orderId must not be null");
        Objects.requireNonNull(authorId,         "authorId must not be null");

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5, got: " + rating);
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body must not be blank");
        }

        this.id              = id;
        this.listingVariantId = listingVariantId;
        this.skuId           = skuId;
        this.orderId         = orderId;
        this.authorId        = authorId;
        this.authorName      = authorName;
        this.rating          = rating;
        this.title           = title;
        this.body            = body;
        this.pros            = pros;
        this.cons            = cons;
        this.matchingSize    = matchingSize;
        this.photos          = photos != null ? new ArrayList<>(photos) : new ArrayList<>();
        this.status          = status != null ? status : ReviewStatus.PENDING;
        this.sellerReply     = sellerReply;
        this.sellerRepliedAt = sellerRepliedAt;
        this.createdAt       = createdAt != null ? createdAt : Instant.now();
        this.updatedAt       = updatedAt != null ? updatedAt : this.createdAt;
    }

    // ── Domain mutations ──────────────────────────────────────────────────────

    /** Approves the review, making it visible on the storefront. */
    public void approve() {
        this.status    = ReviewStatus.APPROVED;
        this.updatedAt = Instant.now();
    }

    /** Rejects the review — it will never appear publicly. */
    public void reject() {
        this.status    = ReviewStatus.REJECTED;
        this.updatedAt = Instant.now();
    }

    /**
     * Posts a public seller reply to the review.
     * Only valid on an already-approved review.
     */
    public void postReply(String replyText) {
        if (replyText == null || replyText.isBlank()) {
            throw new IllegalArgumentException("replyText must not be blank");
        }
        this.sellerReply     = replyText;
        this.sellerRepliedAt = Instant.now();
        this.updatedAt       = Instant.now();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long         getId()              { return id; }
    public Long         getListingVariantId() { return listingVariantId; }
    public Long         getSkuId()           { return skuId; }
    public Long         getOrderId()         { return orderId; }
    public Long         getAuthorId()        { return authorId; }
    public String       getAuthorName()      { return authorName; }
    public int          getRating()          { return rating; }
    public String       getTitle()           { return title; }
    public String       getBody()            { return body; }
    public String       getPros()            { return pros; }
    public String       getCons()            { return cons; }
    public MatchingSize getMatchingSize()    { return matchingSize; }
    /** Returns an unmodifiable snapshot of photo URLs. */
    public List<String> getPhotos()          { return List.copyOf(photos); }
    public ReviewStatus getStatus()          { return status; }
    public String       getSellerReply()     { return sellerReply; }
    public Instant      getSellerRepliedAt() { return sellerRepliedAt; }
    public Instant      getCreatedAt()       { return createdAt; }
    public Instant      getUpdatedAt()       { return updatedAt; }
}
