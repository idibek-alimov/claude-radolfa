package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.MarkDeliveryAttemptedUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.CourierAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;

@Service
public class MarkDeliveryAttemptedService implements MarkDeliveryAttemptedUseCase {

    private static final Logger log = LoggerFactory.getLogger(MarkDeliveryAttemptedService.class);

    private final LoadOrderPort            loadOrderPort;
    private final SaveOrderPort            saveOrderPort;
    private final OrderNotificationService orderNotificationService;
    private final int                      maxAttempts;

    public MarkDeliveryAttemptedService(LoadOrderPort loadOrderPort,
                                        SaveOrderPort saveOrderPort,
                                        OrderNotificationService orderNotificationService,
                                        @Value("${radolfa.delivery.max-attempts:3}") int maxAttempts) {
        this.loadOrderPort            = loadOrderPort;
        this.saveOrderPort            = saveOrderPort;
        this.orderNotificationService = orderNotificationService;
        this.maxAttempts              = maxAttempts;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        Order order = loadOrderPort.loadById(command.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + command.orderId()));

        if (order.status() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException(
                    "Order must be OUT_FOR_DELIVERY to record an attempt, current status: " + order.status());
        }

        if (order.courierId() == null || !order.courierId().equals(command.courierId())) {
            throw new CourierAccessDeniedException(
                    "Courier " + command.courierId() + " is not assigned to order " + command.orderId());
        }

        int newAttemptCount = order.deliveryAttemptCount() + 1;

        Order updated = order.toBuilder()
                .status(OrderStatus.DELIVERY_ATTEMPTED)
                .deliveryAttemptedAt(Instant.now())
                .deliveryAttemptCount(newAttemptCount)
                .deliveryAttemptReason(command.reason())
                .deliveryPhotoUrl(command.photoUrl())
                .build();

        saveOrderPort.save(updated);
        orderNotificationService.notify(updated);

        if (newAttemptCount >= maxAttempts) {
            log.warn("[DELIVERY] Retry limit reached for orderId={} count={} — admin alert pending",
                    command.orderId(), newAttemptCount);
        }
    }
}
