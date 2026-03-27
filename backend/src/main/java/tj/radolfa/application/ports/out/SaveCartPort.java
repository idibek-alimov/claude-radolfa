package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Cart;

/**
 * Out-Port: persist a shopping cart (insert or update).
 */
public interface SaveCartPort {

    /**
     * Saves the cart and returns the persisted instance (with generated ID if new).
     */
    Cart save(Cart cart);
}
