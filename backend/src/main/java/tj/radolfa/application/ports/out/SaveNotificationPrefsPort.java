package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.NotificationPreferences;

public interface SaveNotificationPrefsPort {

    /** Upserts the given preferences (one row per {@code userId}). */
    NotificationPreferences save(NotificationPreferences prefs);
}
