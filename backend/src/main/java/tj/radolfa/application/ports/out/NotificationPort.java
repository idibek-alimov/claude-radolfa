package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;

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

    /**
     * Notifies the reviewer that their review was approved and is now public.
     *
     * @param userId   the review author
     * @param reviewId the approved review
     */
    void sendReviewApprovedNotification(Long userId, Long reviewId);

    /**
     * Notifies the reviewer that the seller posted a reply to their review.
     *
     * @param userId   the review author
     * @param reviewId the review that received a reply
     */
    void sendReviewReplyNotification(Long userId, Long reviewId);

    /**
     * Sends the 6-digit delivery verification code to the customer.
     *
     * @param userId    the recipient (customer)
     * @param orderId   the order being handed off
     * @param code      the 6-digit code
     * @param expiresAt when the code expires
     */
    void sendDeliveryCode(Long userId, Long orderId, String code, Instant expiresAt);

    /**
     * Warns the customer that their pickpoint order will expire soon.
     *
     * @param userId       the recipient (customer)
     * @param orderId      the READY_FOR_PICKUP order
     * @param daysRemaining days until the order is auto-cancelled
     */
    void sendPickpointExpiryWarning(Long userId, Long orderId, int daysRemaining);

    /**
     * Notifies the customer that their pickpoint order has been auto-cancelled
     * due to the pickup window expiring.
     *
     * @param userId  the recipient (customer)
     * @param orderId the cancelled order
     */
    void sendPickpointOrderExpiredCancellation(Long userId, Long orderId);

    /**
     * Notifies the customer that their uncollected pickpoint order is being returned
     * to the warehouse. Triggered when staff or admin manually initiates the return.
     *
     * @param userId  the recipient (customer)
     * @param orderId the order being returned
     */
    default void sendReturnInitiatedNotification(Long userId, Long orderId) {}

    /**
     * Notifies the customer that their walk-in return has been received at the pickup point.
     * Triggered when pickpoint staff logs a customer return.
     *
     * @param userId  the recipient (customer)
     * @param orderId the returned order
     */
    default void sendCustomerReturnReceivedNotification(Long userId, Long orderId) {}
}
