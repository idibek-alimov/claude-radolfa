package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.domain.model.Order;

/**
 * Dispatches customer-facing notifications when an order changes to a
 * customer-relevant state (PAID, SHIPPED, READY_FOR_PICKUP, DELIVERED, CANCELLED).
 *
 * <p>Best-effort: a notification failure is logged at WARN and swallowed so that a
 * transient provider error never rolls back an order's status transition.
 */
@Service
public class OrderNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationService.class);

    private final NotificationPort notificationPort;

    public OrderNotificationService(NotificationPort notificationPort) {
        this.notificationPort = notificationPort;
    }

    public void notify(Order order) {
        try {
            switch (order.status()) {
                case PAID ->
                        notificationPort.sendOrderConfirmation(order.userId(), order.id());
                case SHIPPED, READY_FOR_PICKUP, DELIVERED, CANCELLED, REFUNDED ->
                        notificationPort.sendOrderStatusUpdate(order.userId(), order.id(), order.status());
                default -> { /* PENDING: no notification */ }
            }
        } catch (RuntimeException ex) {
            log.warn("Notification failed for order id={} status={}: {}",
                    order.id(), order.status(), ex.getMessage());
        }
    }
}
