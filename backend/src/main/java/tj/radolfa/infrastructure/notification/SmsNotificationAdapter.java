package tj.radolfa.infrastructure.notification;

// TODO: Replace with real SMS provider implementation.
// Recommended providers for Tajikistan: Eskiz (eskiz.uz), SMS.ru, or direct GSM gateway.
// Activate by adding 'sms' to SPRING_PROFILES_ACTIVE and implementing the HTTP calls below.
// Phone numbers are resolved via LoadUserPort: user.phone().value()

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;

@Component
@Profile("sms")
public class SmsNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationAdapter.class);

    private final LoadUserPort loadUserPort;

    public SmsNotificationAdapter(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    @Override
    public void sendOrderConfirmation(Long userId, Long orderId) {
        // TODO: loadUserPort.loadById(userId).map(u -> u.phone().value()) → send SMS
        log.warn("[SMS PLACEHOLDER] Would send order confirmation to userId={} orderId={}", userId, orderId);
    }

    @Override
    public void sendOrderStatusUpdate(Long userId, Long orderId, OrderStatus newStatus) {
        // TODO: loadUserPort.loadById(userId).map(u -> u.phone().value()) → send SMS
        log.warn("[SMS PLACEHOLDER] Would send status update to userId={} orderId={} status={}",
                userId, orderId, newStatus);
    }

    @Override
    public void sendReviewApprovedNotification(Long userId, Long reviewId) {
        // TODO: loadUserPort.loadById(userId).map(u -> u.phone().value()) → send SMS
        log.warn("[SMS PLACEHOLDER] Would send review-approved to userId={} reviewId={}", userId, reviewId);
    }

    @Override
    public void sendReviewReplyNotification(Long userId, Long reviewId) {
        // TODO: loadUserPort.loadById(userId).map(u -> u.phone().value()) → send SMS
        log.warn("[SMS PLACEHOLDER] Would send review-reply to userId={} reviewId={}", userId, reviewId);
    }

    @Override
    public void sendDeliveryCode(Long userId, Long orderId, String code, Instant expiresAt) {
        // TODO: loadUserPort.loadById(userId).map(u -> u.phone().value()) → send SMS
        log.warn("[SMS PLACEHOLDER] Would send delivery code to userId={} orderId={} code={} expiresAt={}",
                userId, orderId, code, expiresAt);
    }

    @Override
    public void sendPickpointExpiryWarning(Long userId, Long orderId, int daysRemaining) {
        // TODO: loadUserPort.loadById(userId).map(u -> u.phone().value()) → send SMS
        log.warn("[SMS PLACEHOLDER] Would send pickpoint expiry warning to userId={} orderId={} daysRemaining={}",
                userId, orderId, daysRemaining);
    }

    @Override
    public void sendPickpointOrderExpiredCancellation(Long userId, Long orderId) {
        // TODO: loadUserPort.loadById(userId).map(u -> u.phone().value()) → send SMS
        log.warn("[SMS PLACEHOLDER] Would send pickpoint-expired cancellation to userId={} orderId={}",
                userId, orderId);
    }

    @Override
    public void sendReturnInitiatedNotification(Long userId, Long orderId) {
        // TODO: loadUserPort.loadById(userId).map(u -> u.phone().value()) → send SMS
        log.warn("[SMS PLACEHOLDER] Would send return-initiated notification to userId={} orderId={}",
                userId, orderId);
    }
}
