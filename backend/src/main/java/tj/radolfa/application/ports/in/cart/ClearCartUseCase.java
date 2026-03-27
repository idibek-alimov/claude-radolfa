package tj.radolfa.application.ports.in.cart;

/**
 * In-Port: remove all items from the user's active cart.
 */
public interface ClearCartUseCase {

    /** @param userId the authenticated user's ID */
    void execute(Long userId);
}
