package tj.radolfa.application.ports.in.cart;

import tj.radolfa.domain.model.Cart;

/**
 * In-Port: set the quantity of an existing cart item.
 *
 * <p>If {@code quantity} is 0 or negative the item is removed.
 * Validates stock availability for positive quantities.
 */
public interface UpdateCartItemQuantityUseCase {

    /**
     * @param userId      the authenticated user's ID
     * @param skuId       the SKU whose quantity to change
     * @param newQuantity ≤ 0 removes the item; otherwise replaces the quantity
     * @return the updated cart
     */
    Cart execute(Long userId, Long skuId, int newQuantity);
}
