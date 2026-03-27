package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.Money;

/**
 * In-Port: convert the user's active cart into a PENDING order.
 *
 * <p>Steps performed internally:
 * <ol>
 *   <li>Load and validate the cart (non-empty, all items in stock).</li>
 *   <li>Apply tier discount via {@code LoyaltyCalculator}.</li>
 *   <li>Optionally redeem loyalty points (up to 30% of total).</li>
 *   <li>Create the order and decrement stock.</li>
 *   <li>Transition the cart to CHECKED_OUT.</li>
 * </ol>
 */
public interface CheckoutUseCase {

    /**
     * @param command the checkout request
     * @return a {@link Result} containing the new order ID and price breakdown
     */
    Result execute(Command command);

    record Command(
            Long   userId,
            int    loyaltyPointsToRedeem,  // 0 = no redemption
            String notes                   // optional customer note
    ) {}

    record Result(
            Long  orderId,
            Money subtotal,
            Money tierDiscount,
            Money pointsDiscount,
            Money total
    ) {}
}
