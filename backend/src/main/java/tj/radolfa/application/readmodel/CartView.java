package tj.radolfa.application.readmodel;

import tj.radolfa.domain.model.Money;

import java.util.List;

/**
 * Read model returned by {@code GetCartUseCase}.
 *
 * <p>Enriches each cart line with product display data and live stock status.
 */
public record CartView(
        Long           cartId,       // null when the user has no active cart
        List<ItemView> items,
        Money          total,
        int            itemCount,
        String         couponCode    // null when no coupon applied
) {

    /** An empty view for users with no active cart. */
    public static CartView empty() {
        return new CartView(null, List.of(), Money.ZERO, 0, null);
    }

    public record ItemView(
            Long   skuId,
            String productName,
            String colorName,
            String sizeLabel,
            String imageUrl,
            int    quantity,
            Money  unitPrice,
            Money  lineTotal,
            int    availableStock,   // live stock at read time
            boolean inStock
    ) {}
}
