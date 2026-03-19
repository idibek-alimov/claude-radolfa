package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tj.radolfa.infrastructure.persistence.entity.SyncLogEntity;

/**
 * Spring Data JPA repository for {@link SyncLogEntity}.
 */
public interface SyncLogRepository extends JpaRepository<SyncLogEntity, Long> {
}
