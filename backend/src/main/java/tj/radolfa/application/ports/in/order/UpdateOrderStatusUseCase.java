package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.OrderStatus;

/**
 * In-Port: transition an order to a new status.
 *
 * <p>ADMIN only. Validates the status transition is legal:
 * PENDING → PAID → SHIPPED → DELIVERED.
 *
 * <p>Use {@link CancelOrderUseCase} for the cancellation path.
 */
public interface UpdateOrderStatusUseCase {

    /**
     * @param orderId   the order to update
     * @param newStatus the target status
     */
    void execute(Long orderId, OrderStatus newStatus);
}
