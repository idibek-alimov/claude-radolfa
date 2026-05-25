package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.SaveNotificationFailurePort;
import tj.radolfa.domain.model.NotificationFailure;
import tj.radolfa.infrastructure.persistence.entity.NotificationFailureEntity;
import tj.radolfa.infrastructure.persistence.repository.NotificationFailureRepository;

@Component
public class NotificationFailureRepositoryAdapter implements SaveNotificationFailurePort {

    private final NotificationFailureRepository repository;

    public NotificationFailureRepositoryAdapter(NotificationFailureRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationFailure save(NotificationFailure failure) {
        NotificationFailureEntity entity = new NotificationFailureEntity(
                null,
                failure.notificationType(),
                failure.userId(),
                failure.referenceId(),
                failure.errorMessage(),
                failure.alertSent(),
                failure.failedAt());
        NotificationFailureEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private NotificationFailure toDomain(NotificationFailureEntity e) {
        return new NotificationFailure(
                e.getId(),
                e.getNotificationType(),
                e.getUserId(),
                e.getReferenceId(),
                e.getErrorMessage(),
                e.getFailedAt(),
                e.isAlertSent());
    }
}
