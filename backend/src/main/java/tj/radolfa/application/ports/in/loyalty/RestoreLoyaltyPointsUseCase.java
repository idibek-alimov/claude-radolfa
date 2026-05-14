package tj.radolfa.application.ports.in.loyalty;

/**
 * In-Port: restore loyalty points to a user's balance.
 *
 * <p>Called by {@link tj.radolfa.application.services.CancelOrderService} when an order
 * that included a loyalty-points redemption at checkout is cancelled, so the
 * pessimistically deducted points are returned to the user.
 */
public interface RestoreLoyaltyPointsUseCase {

    /**
     * @param userId        the user whose balance should be credited
     * @param pointsToRestore the number of points to add back (must be positive)
     */
    void execute(Long userId, int pointsToRestore);
}
