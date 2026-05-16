package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GenerateDeliveryCodeUseCase;
import tj.radolfa.application.ports.in.order.UpdateOrderStatusUseCase;
import tj.radolfa.application.ports.out.DeliveryEventPublisher;
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
 * <p>Legal paths: PENDING → PAID → SHIPPED → DELIVERED (home) or READY_FOR_PICKUP → DELIVERED (pickpoint).
 * Admin reschedule: DELIVERY_ATTEMPTED → SHIPPED (re-issues a fresh delivery code automatically).
 * HOME orders transitioning to SHIPPED require {@code courierId}.
 * Cancellation is handled separately by {@link CancelOrderService}.
 * Courier-driven transitions (SHIPPED → OUT_FOR_DELIVERY, OUT_FOR_DELIVERY → DELIVERY_ATTEMPTED)
 * are handled by {@code MarkOutForDeliveryService} and {@code MarkDeliveryAttemptedService}.
 */
@Service
public class UpdateOrderStatusService implements UpdateOrderStatusUseCase {

    private final LoadOrderPort              loadOrderPort;
    private final SaveOrderPort              saveOrderPort;
    private final OrderNotificationService   orderNotificationService;
    private final GenerateDeliveryCodeUseCase generateDeliveryCodeUseCase;
    private final DeliveryEventPublisher     deliveryEventPublisher;

    public UpdateOrderStatusService(LoadOrderPort loadOrderPort,
                                    SaveOrderPort saveOrderPort,
                                    OrderNotificationService orderNotificationService,
                                    GenerateDeliveryCodeUseCase generateDeliveryCodeUseCase,
                                    DeliveryEventPublisher deliveryEventPublisher) {
        this.loadOrderPort              = loadOrderPort;
        this.saveOrderPort              = saveOrderPort;
        this.orderNotificationService   = orderNotificationService;
        this.generateDeliveryCodeUseCase = generateDeliveryCodeUseCase;
        this.deliveryEventPublisher      = deliveryEventPublisher;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        Order order = loadOrderPort.loadById(command.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + command.orderId()));

        validateTransition(order, command.newStatus());
        validateCourierFields(order, command);

        boolean toShipped         = command.newStatus() == OrderStatus.SHIPPED;
        boolean toDelivered       = command.newStatus() == OrderStatus.DELIVERED;
        boolean toReadyForPickup  = command.newStatus() == OrderStatus.READY_FOR_PICKUP;
        Long      courierId       = toShipped ? command.courierId()             : order.courierId();
        String trackingNumber     = toShipped ? command.trackingNumber()        : order.trackingNumber();
        LocalDate edd             = toShipped ? command.estimatedDeliveryDate() : order.estimatedDeliveryDate();
        Instant now               = Instant.now();
        Instant shippedAt         = toShipped        ? now : order.shippedAt();
        Instant deliveredAt       = toDelivered       ? now : order.deliveredAt();
        Instant readyForPickupAt  = toReadyForPickup  ? now : order.readyForPickupAt();

        Order updated = order.toBuilder()
                .status(command.newStatus())
                .courierId(courierId)
                .trackingNumber(trackingNumber)
                .estimatedDeliveryDate(edd)
                .shippedAt(shippedAt)
                .deliveredAt(deliveredAt)
                .readyForPickupAt(readyForPickupAt)
                .build();
        saveOrderPort.save(updated);

        if (command.newStatus() == OrderStatus.SHIPPED || command.newStatus() == OrderStatus.READY_FOR_PICKUP) {
            generateDeliveryCodeUseCase.execute(updated.id());
        }

        orderNotificationService.notify(updated);

        // WebSocket push to field staff
        if (command.newStatus() == OrderStatus.SHIPPED && updated.courierId() != null) {
            deliveryEventPublisher.publishOrderAssignedToCourier(updated.courierId(), updated.id());
        } else if (command.newStatus() == OrderStatus.READY_FOR_PICKUP && updated.pickpointId() != null) {
            deliveryEventPublisher.publishNewOrderAtPickpoint(updated.pickpointId(), updated.id());
        }
    }

    private void validateCourierFields(Order order, Command command) {
        if (command.newStatus() == OrderStatus.SHIPPED
                && order.deliveryType() == DeliveryType.HOME
                && command.courierId() == null) {
            throw new IllegalArgumentException("Courier ID is required when shipping a home delivery");
        }
    }

    private void validateTransition(Order order, OrderStatus to) {
        if (to == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Use the cancel endpoint to cancel an order.");
        }
        boolean pickpoint = order.deliveryType() == DeliveryType.PICKPOINT;
        boolean valid = switch (order.status()) {
            case PENDING            -> to == OrderStatus.PAID;
            case PAID               -> pickpoint ? to == OrderStatus.READY_FOR_PICKUP
                                                 : to == OrderStatus.SHIPPED;
            case SHIPPED            -> !pickpoint && to == OrderStatus.DELIVERED;
            case READY_FOR_PICKUP   -> pickpoint  && to == OrderStatus.DELIVERED;
            case DELIVERY_ATTEMPTED -> !pickpoint && to == OrderStatus.SHIPPED;
            default                 -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Invalid status transition: " + order.status() + " → " + to
                    + " (deliveryType=" + order.deliveryType() + ")");
        }
    }
}
