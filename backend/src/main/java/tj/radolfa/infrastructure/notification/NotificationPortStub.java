package tj.radolfa.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;

/**
 * Stub for {@link NotificationPort} — logs all calls to the console.
 *
 * <p>Active when the {@code sms} profile is NOT active. When the {@code sms} profile
 * is enabled, {@link SmsNotificationAdapter} takes over and this stub is excluded.
 */
@Component
@Profile("!sms")
public class NotificationPortStub implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(NotificationPortStub.class);

    @Override
    public void sendOrderConfirmation(Long userId, Long orderId) {
        log.info("[NOTIFICATION STUB] Order confirmation → userId={} orderId={}", userId, orderId);
    }

    @Override
    public void sendOrderStatusUpdate(Long userId, Long orderId, OrderStatus newStatus) {
        log.info("[NOTIFICATION STUB] Order status update → userId={} orderId={} status={}",
                userId, orderId, newStatus);
    }

    @Override
    public void sendReviewApprovedNotification(Long userId, Long reviewId) {
        log.info("[NOTIFICATION STUB] Review approved → userId={} reviewId={}", userId, reviewId);
    }

    @Override
    public void sendReviewReplyNotification(Long userId, Long reviewId) {
        log.info("[NOTIFICATION STUB] Seller reply posted → userId={} reviewId={}", userId, reviewId);
    }

    @Override
    public void sendDeliveryCode(Long userId, Long orderId, String code, Instant expiresAt) {
        log.info("[NOTIFICATION STUB] Delivery code → userId={} orderId={} code={} expiresAt={}",
                userId, orderId, code, expiresAt);
    }

    @Override
    public void sendPickpointExpiryWarning(Long userId, Long orderId, int daysRemaining) {
        log.info("[NOTIFICATION STUB] Pickpoint expiry warning → userId={} orderId={} daysRemaining={}",
                userId, orderId, daysRemaining);
    }

    @Override
    public void sendPickpointOrderExpiredCancellation(Long userId, Long orderId) {
        log.info("[NOTIFICATION STUB] Pickpoint order expired cancellation → userId={} orderId={}",
                userId, orderId);
    }

    @Override
    public void sendReturnInitiatedNotification(Long userId, Long orderId) {
        log.info("[NOTIFICATION STUB] Return initiated → userId={} orderId={}", userId, orderId);
    }

    @Override
    public void sendCustomerReturnReceivedNotification(Long userId, Long orderId) {
        log.info("[NOTIFICATION STUB] Customer return received → userId={} orderId={}", userId, orderId);
    }
}
