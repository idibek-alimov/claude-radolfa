package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.MarkOutForDeliveryUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.CourierAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;

@Service
public class MarkOutForDeliveryService implements MarkOutForDeliveryUseCase {

    private final LoadOrderPort            loadOrderPort;
    private final SaveOrderPort            saveOrderPort;
    private final OrderNotificationService orderNotificationService;

    public MarkOutForDeliveryService(LoadOrderPort loadOrderPort,
                                     SaveOrderPort saveOrderPort,
                                     OrderNotificationService orderNotificationService) {
        this.loadOrderPort            = loadOrderPort;
        this.saveOrderPort            = saveOrderPort;
        this.orderNotificationService = orderNotificationService;
    }

    @Override
    @Transactional
    public void execute(Long orderId, Long courierId) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.status() != OrderStatus.SHIPPED) {
            throw new IllegalStateException(
                    "Order must be SHIPPED to mark as out for delivery, current status: " + order.status());
        }

        if (order.courierId() == null || !order.courierId().equals(courierId)) {
            throw new CourierAccessDeniedException(
                    "Courier " + courierId + " is not assigned to order " + orderId);
        }

        Order updated = order.toBuilder()
                .status(OrderStatus.OUT_FOR_DELIVERY)
                .outForDeliveryAt(Instant.now())
                .build();

        saveOrderPort.save(updated);
        orderNotificationService.notify(updated);
    }
}
