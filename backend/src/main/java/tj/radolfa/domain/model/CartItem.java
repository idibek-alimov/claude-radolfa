package tj.radolfa.domain.model;

import java.util.Objects;

/**
 * A single line in a shopping cart.
 *
 * <p>Mutable — quantity can change while the cart is active.
 * {@code unitPriceSnapshot} is the price captured at the moment the item was
 * added and never changes, protecting the cart from mid-session price shifts.
 *
 * <p>Pure Java — zero framework dependencies.
 */
public class CartItem {

    private final Long  skuId;
    private       int   quantity;
    private final Money unitPriceSnapshot;

    public CartItem(Long skuId, int quantity, Money unitPriceSnapshot) {
        Objects.requireNonNull(skuId,              "skuId must not be null");
        Objects.requireNonNull(unitPriceSnapshot,  "unitPriceSnapshot must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0, got: " + quantity);
        }
        this.skuId             = skuId;
        this.quantity          = quantity;
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    /** Replaces the quantity. Must be > 0. */
    public void updateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0, got: " + quantity);
        }
        this.quantity = quantity;
    }

    /** Price of this line: unit price × quantity. */
    public Money lineTotal() {
        return unitPriceSnapshot.multiply(quantity);
    }

    public Long  getSkuId()             { return skuId; }
    public int   getQuantity()          { return quantity; }
    public Money getUnitPriceSnapshot() { return unitPriceSnapshot; }
}
