package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.NotificationFailureEntity;

public interface NotificationFailureRepository extends JpaRepository<NotificationFailureEntity, Long> {
}
