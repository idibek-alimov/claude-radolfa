package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.NotificationPrefsEntity;

import java.util.Optional;

public interface NotificationPrefsRepository extends JpaRepository<NotificationPrefsEntity, Long> {

    Optional<NotificationPrefsEntity> findByUserId(Long userId);
}
