package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.out.AdminAlertPort;
import tj.radolfa.application.ports.out.NotificationPort;

import java.time.Instant;

@Service
public class DeliveryCodeNotificationService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryCodeNotificationService.class);
    private static final String TYPE = "DELIVERY_CODE";

    private final NotificationPort notificationPort;
    private final AdminAlertPort adminAlertPort;
    private final RetryTemplate retryTemplate;

    public DeliveryCodeNotificationService(
            NotificationPort notificationPort,
            AdminAlertPort adminAlertPort,
            @Qualifier("deliveryCodeRetryTemplate") RetryTemplate retryTemplate) {
        this.notificationPort = notificationPort;
        this.adminAlertPort   = adminAlertPort;
        this.retryTemplate    = retryTemplate;
    }

    public void send(Long userId, Long orderId, String code, Instant expiresAt) {
        try {
            retryTemplate.execute(ctx -> {
                notificationPort.sendDeliveryCode(userId, orderId, code, expiresAt);
                return null;
            });
        } catch (Exception ex) {
            log.error("[NOTIFICATION FAILURE] All retries exhausted for delivery code. orderId={} userId={}",
                    orderId, userId, ex);
            adminAlertPort.sendNotificationFailureAlert(TYPE, userId, orderId, ex.getMessage());
        }
    }
}
