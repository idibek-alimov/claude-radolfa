package tj.radolfa.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.AdminAlertPort;
import tj.radolfa.application.ports.out.SaveNotificationFailurePort;
import tj.radolfa.domain.model.NotificationFailure;

import java.time.Instant;

@Component
public class AdminAlertPortStub implements AdminAlertPort {

    private static final Logger log = LoggerFactory.getLogger(AdminAlertPortStub.class);

    private final SaveNotificationFailurePort saveFailurePort;

    public AdminAlertPortStub(SaveNotificationFailurePort saveFailurePort) {
        this.saveFailurePort = saveFailurePort;
    }

    @Override
    public void sendNotificationFailureAlert(String type, Long userId, Long refId, String error) {
        log.error("[ADMIN ALERT] Notification failure — type={} userId={} refId={} error={}",
                type, userId, refId, error);
        saveFailurePort.save(new NotificationFailure(
                null, type, userId, refId, error, Instant.now(), true));
    }
}
