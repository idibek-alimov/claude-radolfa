package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The user's shopping cart — a mutable aggregate root.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 *
 * <p>Cart mutation methods enforce all business invariants. Specifically:
 * <ul>
 *   <li>Adding a duplicate SKU increases quantity rather than creating a second line.</li>
 *   <li>Updating to quantity {@code <= 0} removes the item (clear-via-update pattern).</li>
 *   <li>Removing a SKU that is not in the cart throws {@link IllegalArgumentException}.</li>
 * </ul>
 */
public class Cart {

    private final Long userId;
    private final List<CartItem> items;

    public Cart(Long userId, List<CartItem> items) {
        if (userId == null) {
            throw new IllegalArgumentException("Cart userId must not be null");
        }
        this.userId = userId;
        this.items = new ArrayList<>(items != null ? items : List.of());
    }

    /**
     * Adds an item to the cart. If a line for {@code skuId} already exists,
     * the quantity is increased by {@code quantity} instead of creating a new line.
     *
     * @throws IllegalArgumentException if quantity is not positive
     */
    public void addItem(Long skuId,
                        String listingSlug,
                        String productName,
                        String sizeLabel,
                        String imageUrl,
                        BigDecimal priceSnapshot,
                        int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive, got: " + quantity);
        }
        Optional<CartItem> existing = findBySku(skuId);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            items.add(new CartItem(skuId, listingSlug, productName, sizeLabel, imageUrl, priceSnapshot, quantity));
        }
    }

    /**
     * Updates the quantity for an existing cart item.
     * If {@code quantity <= 0} the item is removed from the cart.
     *
     * @throws IllegalArgumentException if skuId is not found in the cart
     */
    public void updateQuantity(Long skuId, int quantity) {
        CartItem item = findBySku(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found in cart: " + skuId));
        if (quantity <= 0) {
            items.remove(item);
        } else {
            item.setQuantity(quantity);
        }
    }

    /**
     * Removes an item from the cart entirely.
     *
     * @throws IllegalArgumentException if skuId is not found in the cart
     */
    public void removeItem(Long skuId) {
        CartItem item = findBySku(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found in cart: " + skuId));
        items.remove(item);
    }

    /**
     * Empties the cart.
     */
    public void clear() {
        items.clear();
    }

    /**
     * Returns the total value of all items in the cart.
     */
    public Money subtotal() {
        return items.stream()
                .map(CartItem::itemSubtotal)
                .reduce(Money.ZERO, Money::add);
    }

    // ---- Private helpers ----

    private Optional<CartItem> findBySku(Long skuId) {
        return items.stream()
                .filter(item -> item.getSkuId().equals(skuId))
                .findFirst();
    }

    // ---- Getters ----
    public Long           getUserId() { return userId; }
    public List<CartItem> getItems()  { return Collections.unmodifiableList(items); }
}
