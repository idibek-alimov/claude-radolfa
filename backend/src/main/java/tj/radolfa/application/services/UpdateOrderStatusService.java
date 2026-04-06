package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.UpdateOrderStatusUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

/**
 * ADMIN-only service: transitions an order through the fulfilment pipeline.
 *
 * <p>Legal forward path: PENDING → PAID → SHIPPED → DELIVERED.
 * Cancellation is handled separately by {@link CancelOrderService}.
 */
@Service
public class UpdateOrderStatusService implements UpdateOrderStatusUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;

    public UpdateOrderStatusService(LoadOrderPort loadOrderPort,
                                    SaveOrderPort saveOrderPort) {
        this.loadOrderPort = loadOrderPort;
        this.saveOrderPort = saveOrderPort;
    }

    @Override
    @Transactional
    public void execute(Long orderId, OrderStatus newStatus) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        validateTransition(order.status(), newStatus);

        Order updated = new Order(order.id(), order.userId(), order.externalOrderId(),
                newStatus, order.totalAmount(), order.items(), order.createdAt(),
                order.loyaltyPointsRedeemed(), order.loyaltyPointsAwarded());
        saveOrderPort.save(updated);
    }

    private void validateTransition(OrderStatus from, OrderStatus to) {
        if (to == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Use the cancel endpoint to cancel an order.");
        }
        boolean valid = switch (from) {
            case PENDING  -> to == OrderStatus.PAID;
            case PAID     -> to == OrderStatus.SHIPPED;
            case SHIPPED  -> to == OrderStatus.DELIVERED;
            default       -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Invalid status transition: " + from + " → " + to);
        }
    }
}
