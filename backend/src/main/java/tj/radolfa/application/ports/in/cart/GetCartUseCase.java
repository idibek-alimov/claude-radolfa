package tj.radolfa.application.ports.in.cart;

import tj.radolfa.application.readmodel.CartView;

/**
 * In-Port: retrieve the user's active cart, enriched with live stock status.
 *
 * <p>Returns an empty cart view if the user has no active cart yet.
 */
public interface GetCartUseCase {

    /**
     * @param userId the authenticated user's ID
     * @return the cart view; never null (returns an empty view if no cart exists)
     */
    CartView execute(Long userId);
}
