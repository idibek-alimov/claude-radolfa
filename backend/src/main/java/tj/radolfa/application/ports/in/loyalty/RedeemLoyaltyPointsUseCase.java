package tj.radolfa.application.ports.in.loyalty;

import tj.radolfa.domain.model.Money;

/**
 * In-Port: deduct loyalty points from a user's balance during checkout.
 *
 * <p>Called by {@code CheckoutUseCase} when the user opts to redeem points.
 * Points are deducted pessimistically (before payment) to prevent
 * double-spend across concurrent sessions.
 *
 * <p>If payment fails, {@code RefundPaymentUseCase} is responsible for
 * restoring the deducted points.
 */
public interface RedeemLoyaltyPointsUseCase {

    /**
     * Deducts points and returns the equivalent monetary discount.
     *
     * @param userId         the user redeeming points
     * @param pointsToRedeem must be ≤ {@code LoyaltyCalculator.maxRedeemablePoints}
     * @return the monetary value of the redeemed points
     */
    Money execute(Long userId, int pointsToRedeem);
}
