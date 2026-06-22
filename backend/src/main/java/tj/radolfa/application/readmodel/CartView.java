package tj.radolfa.application.readmodel;

import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.WinningMechanism;

import java.util.List;

/**
 * Read model returned by {@code GetCartUseCase}.
 *
 * <p>Enriches each cart line with product display data, live stock status, and
 * display pricing resolved via {@code CartLinePricer} — the same campaign-vs-loyalty
 * best-of decision that {@code CheckoutService} charges, so the cart never displays
 * a price different from what checkout would actually charge.
 *
 * <p>{@code total} is the discounted total (equal to {@code summary.total()}) — the
 * amount checkout would charge for this cart right now.
 */
public record CartView(
        Long           cartId,          // null when the user has no active cart
        List<ItemView> items,
        Money          total,           // discounted total — equals summary.total()
        int            itemCount,
        String         couponCode,      // null when no coupon applied
        Long           pendingOrderId,  // non-null when checkout was initiated but payment not yet confirmed
        Summary        summary
) {

    /** An empty view for users with no active cart. */
    public static CartView empty() {
        return new CartView(null, List.of(), Money.ZERO, 0, null, null, Summary.empty());
    }

    public record ItemView(
            Long   skuId,
            String productName,
            String colorName,
            String sizeLabel,
            String imageUrl,
            int    quantity,
            Money  unitPrice,           // final (discounted) unit price
            Money  lineTotal,           // unitPrice × quantity
            int    availableStock,      // live stock at read time
            boolean inStock,
            Money  originalUnitPrice,   // pre-discount snapshot price
            Integer discountPercent,    // null when mechanism == NONE
            WinningMechanism mechanism,
            String category,            // nullable — not every product has one set
            String productCode          // nullable — links to /products/{productCode}
    ) {}

    /**
     * Order summary breakdown. Savings are partitioned by which mechanism won each
     * line — a campaign discount and the loyalty tier discount are never stacked, so
     * {@code itemDiscounts} and {@code crownTier} never double-count the same line.
     *
     * <p>Invariant: {@code subtotal - total == itemDiscounts + crownTier}, exactly.
     */
    public record Summary(
            Money subtotal,        // Σ originalUnitPrice × qty (equals cart.total())
            Money itemDiscounts,   // Σ savings on lines where CAMPAIGN won
            Money crownTier,       // Σ savings on lines where LOYALTY won
            Money shipping,        // always ZERO — shipping is free today
            Money total,           // Σ unitPrice × qty (final/discounted)
            Money savings          // itemDiscounts + crownTier
    ) {
        public static Summary empty() {
            return new Summary(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO);
        }
    }
}
