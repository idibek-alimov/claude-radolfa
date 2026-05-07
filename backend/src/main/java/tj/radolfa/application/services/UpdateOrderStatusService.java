package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.UpdateOrderStatusUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * ADMIN-only service: transitions an order through the fulfilment pipeline.
 *
 * <p>Legal forward path: PENDING → PAID → SHIPPED → DELIVERED.
 * HOME orders transitioning to SHIPPED require {@code courierName}.
 * Cancellation is handled separately by {@link CancelOrderService}.
 */
@Service
public class UpdateOrderStatusService implements UpdateOrderStatusUseCase {

    private final LoadOrderPort            loadOrderPort;
    private final SaveOrderPort            saveOrderPort;
    private final OrderNotificationService orderNotificationService;

    public UpdateOrderStatusService(LoadOrderPort loadOrderPort,
                                    SaveOrderPort saveOrderPort,
                                    OrderNotificationService orderNotificationService) {
        this.loadOrderPort            = loadOrderPort;
        this.saveOrderPort            = saveOrderPort;
        this.orderNotificationService = orderNotificationService;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        Order order = loadOrderPort.loadById(command.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + command.orderId()));

        validateTransition(order, command.newStatus());
        validateCourierFields(order, command);

        boolean toShipped   = command.newStatus() == OrderStatus.SHIPPED;
        boolean toDelivered = command.newStatus() == OrderStatus.DELIVERED;
        String courierName    = toShipped ? command.courierName()           : order.courierName();
        String trackingNumber = toShipped ? command.trackingNumber()        : order.trackingNumber();
        LocalDate edd         = toShipped ? command.estimatedDeliveryDate() : order.estimatedDeliveryDate();
        Instant now           = Instant.now();
        Instant shippedAt     = toShipped   ? now : order.shippedAt();
        Instant deliveredAt   = toDelivered ? now : order.deliveredAt();

        Order updated = new Order(
                order.id(), order.userId(), order.externalOrderId(),
                command.newStatus(), order.totalAmount(), order.items(), order.createdAt(),
                order.loyaltyPointsRedeemed(), order.loyaltyPointsAwarded(),
                order.deliveryType(), order.deliveryAddress(), order.preferredTimeWindow(), order.pickpointId(),
                courierName, trackingNumber, edd,
                shippedAt, deliveredAt, order.cancelledAt());
        saveOrderPort.save(updated);
        orderNotificationService.notify(updated);
    }

    private void validateCourierFields(Order order, Command command) {
        if (command.newStatus() == OrderStatus.SHIPPED
                && order.deliveryType() == DeliveryType.HOME
                && (command.courierName() == null || command.courierName().isBlank())) {
            throw new IllegalArgumentException("Courier name is required when shipping a home delivery");
        }
    }

    private void validateTransition(Order order, OrderStatus to) {
        if (to == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Use the cancel endpoint to cancel an order.");
        }
        boolean pickpoint = order.deliveryType() == DeliveryType.PICKPOINT;
        boolean valid = switch (order.status()) {
            case PENDING          -> to == OrderStatus.PAID;
            case PAID             -> pickpoint ? to == OrderStatus.READY_FOR_PICKUP
                                               : to == OrderStatus.SHIPPED;
            case SHIPPED          -> !pickpoint && to == OrderStatus.DELIVERED;
            case READY_FOR_PICKUP -> pickpoint  && to == OrderStatus.DELIVERED;
            default               -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Invalid status transition: " + order.status() + " → " + to
                    + " (deliveryType=" + order.deliveryType() + ")");
        }
    }
}
