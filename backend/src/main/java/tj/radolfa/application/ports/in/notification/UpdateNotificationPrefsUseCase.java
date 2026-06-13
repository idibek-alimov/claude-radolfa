package tj.radolfa.application.ports.in.notification;

import tj.radolfa.domain.model.NotificationPreferences;

public interface UpdateNotificationPrefsUseCase {

    NotificationPreferences execute(Command command);

    record Command(Long userId,
                    boolean orderUpdates,
                    boolean promotions,
                    boolean smsMessages) {}
}
