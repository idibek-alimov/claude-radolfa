package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.OrderStatus;

import java.time.LocalDate;

/**
 * In-Port: transition an order to a new status.
 *
 * <p>ADMIN only. Validates the status transition is legal:
 * PENDING → PAID → SHIPPED → DELIVERED.
 *
 * <p>For HOME orders transitioning to SHIPPED, {@code courierId} is required.
 * Use {@link CancelOrderUseCase} for the cancellation path.
 */
public interface UpdateOrderStatusUseCase {

    void execute(Command command);

    record Command(
            Long orderId,
            OrderStatus newStatus,
            Long courierId,
            String trackingNumber,
            LocalDate estimatedDeliveryDate) {}
}
