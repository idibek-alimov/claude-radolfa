package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.notification.GetNotificationPrefsUseCase;
import tj.radolfa.application.ports.in.notification.UpdateNotificationPrefsUseCase;
import tj.radolfa.application.ports.out.LoadNotificationPrefsPort;
import tj.radolfa.application.ports.out.SaveNotificationPrefsPort;
import tj.radolfa.domain.model.NotificationPreferences;

@Service
public class NotificationPrefsService implements GetNotificationPrefsUseCase, UpdateNotificationPrefsUseCase {

    private final LoadNotificationPrefsPort loadNotificationPrefsPort;
    private final SaveNotificationPrefsPort saveNotificationPrefsPort;

    public NotificationPrefsService(LoadNotificationPrefsPort loadNotificationPrefsPort,
                                     SaveNotificationPrefsPort saveNotificationPrefsPort) {
        this.loadNotificationPrefsPort = loadNotificationPrefsPort;
        this.saveNotificationPrefsPort = saveNotificationPrefsPort;
    }

    @Override
    @Transactional
    public NotificationPreferences execute(Long userId) {
        return loadNotificationPrefsPort.findByUserId(userId)
                .orElseGet(() -> NotificationPreferences.defaults(userId));
    }

    @Override
    @Transactional
    public NotificationPreferences execute(Command command) {
        NotificationPreferences prefs = new NotificationPreferences(
                command.userId(),
                command.orderUpdates(),
                command.promotions(),
                command.smsMessages());

        return saveNotificationPrefsPort.save(prefs);
    }
}
