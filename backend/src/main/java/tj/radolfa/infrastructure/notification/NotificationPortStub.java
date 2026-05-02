package tj.radolfa.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.domain.model.OrderStatus;

/**
 * Stub for {@link NotificationPort} — logs all calls to the console.
 *
 * <p>Active in all profiles until a real SMS/push adapter is provided.
 */
@Component
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
}
