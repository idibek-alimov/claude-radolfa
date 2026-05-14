package tj.radolfa.application.ports.in.loyalty;

/**
 * In-Port: revoke cashback points that were awarded after a payment that is now refunded.
 *
 * <p>Called by {@link tj.radolfa.application.services.RefundPaymentService} to deduct
 * the cashback points that were credited to the user when the payment was confirmed.
 * The user's balance is floored at zero — it can never go negative.
 */
public interface RevokeAwardedPointsUseCase {

    /**
     * @param userId         the user whose balance should be debited
     * @param pointsToRevoke the number of points to deduct (must be positive)
     */
    void execute(Long userId, int pointsToRevoke);
}
