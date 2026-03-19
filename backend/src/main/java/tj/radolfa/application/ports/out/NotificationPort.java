package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.OrderStatus;

/**
 * Out-Port: send notifications to users.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@code NotificationPortStub} — dev/test; logs to console.</li>
 *   <li>Future: SMS via SMS.ru, push via FCM, or email via SMTP.</li>
 * </ul>
 */
public interface NotificationPort {

    /**
     * Notifies the user that their order was placed successfully.
     *
     * @param userId  the recipient
     * @param orderId the new order's ID
     */
    void sendOrderConfirmation(Long userId, Long orderId);

    /**
     * Notifies the user that their order status changed.
     *
     * @param userId    the recipient
     * @param orderId   the affected order
     * @param newStatus the status the order transitioned to
     */
    void sendOrderStatusUpdate(Long userId, Long orderId, OrderStatus newStatus);
}
