package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.NotificationFailure;

public interface SaveNotificationFailurePort {
    NotificationFailure save(NotificationFailure failure);
}
