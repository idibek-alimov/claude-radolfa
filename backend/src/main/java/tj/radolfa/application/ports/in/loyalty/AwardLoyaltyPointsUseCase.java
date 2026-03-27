package tj.radolfa.application.ports.in.loyalty;

/**
 * In-Port: award cashback points to a user after a successful payment.
 *
 * <p>Called internally by {@code ConfirmPaymentUseCase} after the order
 * transitions to PAID. Uses {@code LoyaltyCalculator} to determine the
 * earned points and whether a tier upgrade applies.
 */
public interface AwardLoyaltyPointsUseCase {

    /**
     * @param userId  the user to award points to
     * @param orderId the order that triggered the award (for audit)
     */
    void execute(Long userId, Long orderId);
}
