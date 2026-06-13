package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.NotificationPreferences;

import java.util.Optional;

public interface LoadNotificationPrefsPort {

    Optional<NotificationPreferences> findByUserId(Long userId);
}
