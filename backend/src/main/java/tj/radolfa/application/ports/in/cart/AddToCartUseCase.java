package tj.radolfa.application.ports.in.cart;

import tj.radolfa.domain.model.Cart;

/**
 * In-Port: add a SKU to the user's active cart.
 *
 * <p>If the SKU is already in the cart the quantities are merged.
 * Validates that the requested quantity does not exceed available stock.
 */
public interface AddToCartUseCase {

    /**
     * @param userId   the authenticated user's ID
     * @param skuId    the SKU to add
     * @param quantity must be ≥ 1
     * @return the updated cart
     */
    Cart execute(Long userId, Long skuId, int quantity);
}
