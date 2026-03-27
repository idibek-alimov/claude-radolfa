package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Cart;

import java.util.Optional;

/**
 * Out-Port: load a user's shopping cart from the persistence layer.
 */
public interface LoadCartPort {

    /**
     * Returns the user's ACTIVE cart if one exists.
     *
     * @param userId the owner's user ID
     */
    Optional<Cart> findActiveByUserId(Long userId);

    /**
     * Returns any cart by its ID.
     */
    Optional<Cart> findById(Long cartId);
}
