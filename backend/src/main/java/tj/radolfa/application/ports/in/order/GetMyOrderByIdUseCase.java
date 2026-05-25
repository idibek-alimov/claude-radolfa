package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.Order;

public interface GetMyOrderByIdUseCase {
    /**
     * Loads an order for the requesting customer.
     * Throws ResourceNotFoundException if the order does not exist OR belongs to a different user —
     * always 404, never 403, to prevent existence leakage.
     */
    Order execute(Long orderId, Long requestingUserId);
}
