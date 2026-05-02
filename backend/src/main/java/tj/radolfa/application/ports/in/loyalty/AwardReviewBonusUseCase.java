package tj.radolfa.application.ports.in.loyalty;

/**
 * In-Port: award a flat loyalty bonus to a user when their review is approved.
 *
 * <p>Idempotency is handled by the caller: only invoke this when
 * {@code review.pointsAwardedAt == null}.
 */
public interface AwardReviewBonusUseCase {

    /**
     * @param userId the user who wrote the approved review
     */
    void execute(Long userId);
}
