package tj.radolfa.application.ports.in.review;

/**
 * Recomputes the {@code ProductRatingSummary} for a variant from all its approved reviews.
 *
 * <p>Declared in Phase 4 so {@code ModerateReviewService} can depend on it.
 * Implemented in Phase 5 by {@code RecalculateRatingSummaryService}.
 */
public interface RecalculateRatingSummaryUseCase {

    void execute(Long listingVariantId);
}
