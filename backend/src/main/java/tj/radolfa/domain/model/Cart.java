package tj.radolfa.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Shopping cart aggregate.
 *
 * <p>Mutable — items are added, removed, and updated during the user's session.
 * All mutation methods enforce {@link CartStatus#ACTIVE} state before proceeding.
 *
 * <p>Pure Java — zero framework dependencies.
 */
public class Cart {

    private final Long       id;
    private final Long       userId;
    private       CartStatus status;
    private final List<CartItem> items;
    private final Instant    createdAt;
    private       Instant    updatedAt;

    public Cart(Long id,
                Long userId,
                CartStatus status,
                List<CartItem> items,
                Instant createdAt,
                Instant updatedAt) {
        Objects.requireNonNull(userId, "userId must not be null");
        this.id        = id;
        this.userId    = userId;
        this.status    = status    != null ? status    : CartStatus.ACTIVE;
        this.items     = items     != null ? new ArrayList<>(items) : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    /** Factory — creates a brand-new active cart for a user. */
    public static Cart forUser(Long userId) {
        Instant now = Instant.now();
        return new Cart(null, userId, CartStatus.ACTIVE, List.of(), now, now);
    }

    // ── Mutation ──────────────────────────────────────────────────────────────

    /**
     * Adds {@code quantity} units of the given SKU to the cart.
     * If the SKU is already in the cart the quantities are merged.
     */
    public void addItem(Long skuId, int quantity, Money unitPrice) {
        requireActive();
        Optional<CartItem> existing = findItem(skuId);
        if (existing.isPresent()) {
            existing.get().updateQuantity(existing.get().getQuantity() + quantity);
        } else {
            items.add(new CartItem(skuId, quantity, unitPrice));
        }
        touch();
    }

    /** Removes the line for the given SKU. No-op if the SKU is not in the cart. */
    public void removeItem(Long skuId) {
        requireActive();
        items.removeIf(item -> item.getSkuId().equals(skuId));
        touch();
    }

    /**
     * Sets the quantity for a SKU. If {@code quantity} ≤ 0 the item is removed.
     * No-op if the SKU is not in the cart.
     */
    public void updateQuantity(Long skuId, int quantity) {
        requireActive();
        if (quantity <= 0) {
            removeItem(skuId);
            return;
        }
        findItem(skuId).ifPresent(item -> item.updateQuantity(quantity));
        touch();
    }

    /** Removes all items from the cart. */
    public void clear() {
        requireActive();
        items.clear();
        touch();
    }

    /**
     * Transitions status to {@link CartStatus#CHECKED_OUT}.
     * Throws if the cart is empty or already checked out / abandoned.
     */
    public void checkout() {
        requireActive();
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot check out an empty cart.");
        }
        this.status = CartStatus.CHECKED_OUT;
        touch();
    }

    /** Transitions status to {@link CartStatus#ABANDONED}. */
    public void abandon() {
        this.status = CartStatus.ABANDONED;
        touch();
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Sum of all line totals. */
    public Money total() {
        return items.stream()
                .map(CartItem::lineTotal)
                .reduce(Money.ZERO, Money::add);
    }

    /** Total number of individual units across all lines. */
    public int itemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long           getId()        { return id; }
    public Long           getUserId()    { return userId; }
    public CartStatus     getStatus()    { return status; }
    /** Returns an unmodifiable snapshot of the items list. */
    public List<CartItem> getItems()     { return List.copyOf(items); }
    public Instant        getCreatedAt() { return createdAt; }
    public Instant        getUpdatedAt() { return updatedAt; }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Optional<CartItem> findItem(Long skuId) {
        return items.stream().filter(i -> i.getSkuId().equals(skuId)).findFirst();
    }

    private void requireActive() {
        if (status != CartStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cart is not active (status=" + status + "). Cannot mutate.");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
