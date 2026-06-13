package tj.radolfa.application.ports.in.notification;

import tj.radolfa.domain.model.NotificationPreferences;

public interface GetNotificationPrefsUseCase {

    /** Returns the user's saved preferences, or {@link NotificationPreferences#defaults} if none exist yet. */
    NotificationPreferences execute(Long userId);
}
