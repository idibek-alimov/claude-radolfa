package tj.radolfa.domain.service;

import tj.radolfa.domain.model.AppliedDiscount;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.WinningMechanism;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Resolves the best-of unit price for one cart/order line: campaign discount
 * versus the user's loyalty tier discount.
 *
 * <p>Exactly one mechanism is ever applied — never both. A stacked campaign
 * discount wins when its final price ties or beats the loyalty price and
 * strictly beats the original price; otherwise the loyalty price applies
 * (loyalty alone is not recorded as a discount application).
 *
 * <p>This is the single source of truth for the campaign-vs-loyalty decision —
 * shared by checkout (what is charged) and the cart read path (what is
 * displayed) so the two can never diverge.
 *
 * <p>Pure Java — zero framework dependencies. Stateless; safe to share.
 */
public class CartLinePricer {

    /**
     * @param original  the line's original (snapshot) unit price
     * @param tierPct   the user's loyalty tier discount percentage (0 if none)
     * @param discounts the campaign discounts applicable to this line's SKU, in resolution order
     */
    public LinePrice price(Money original, BigDecimal tierPct, List<Discount> discounts) {
        BigDecimal originalAmount = original.amount();

        BigDecimal loyaltyPrice = tierPct.compareTo(BigDecimal.ZERO) > 0
                ? originalAmount.multiply(BigDecimal.ONE.subtract(
                        tierPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                        .setScale(2, RoundingMode.HALF_UP)
                : originalAmount;

        List<AppliedDiscount> applied = List.of();
        BigDecimal finalPrice = loyaltyPrice.min(originalAmount);
        WinningMechanism mechanism = WinningMechanism.NONE;

        if (!discounts.isEmpty()) {
            List<AppliedDiscount> folded = AppliedDiscount.fold(discounts, originalAmount);
            BigDecimal stackedPrice = folded.get(folded.size() - 1).reducedUnitPrice();

            boolean discountWins = stackedPrice.compareTo(loyaltyPrice) <= 0
                    && stackedPrice.compareTo(originalAmount) < 0;

            if (discountWins) {
                applied = folded;
                finalPrice = stackedPrice;
                mechanism = WinningMechanism.CAMPAIGN;
            }
        }

        if (mechanism == WinningMechanism.NONE && finalPrice.compareTo(originalAmount) < 0) {
            mechanism = WinningMechanism.LOYALTY;
        }

        BigDecimal effectivePercent = originalAmount.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.ONE.subtract(finalPrice.divide(originalAmount, 4, RoundingMode.HALF_UP))
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new LinePrice(new Money(finalPrice), mechanism, effectivePercent, applied);
    }

    public record LinePrice(
            Money finalUnitPrice,
            WinningMechanism mechanism,
            BigDecimal effectivePercent,
            List<AppliedDiscount> applied
    ) {}
}
