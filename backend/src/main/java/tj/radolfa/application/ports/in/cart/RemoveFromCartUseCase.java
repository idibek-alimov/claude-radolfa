package tj.radolfa.application.ports.in.cart;

import tj.radolfa.domain.model.Cart;

/**
 * In-Port: remove a SKU line from the user's active cart.
 *
 * <p>No-op if the SKU is not in the cart.
 */
public interface RemoveFromCartUseCase {

    /**
     * @param userId the authenticated user's ID
     * @param skuId  the SKU to remove
     * @return the updated cart
     */
    Cart execute(Long userId, Long skuId);
}
