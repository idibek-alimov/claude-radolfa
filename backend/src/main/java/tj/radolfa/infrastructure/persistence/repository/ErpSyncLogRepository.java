package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tj.radolfa.infrastructure.persistence.entity.ErpSyncLogEntity;

/**
 * Spring Data JPA repository for {@link ErpSyncLogEntity}.
 */
public interface ErpSyncLogRepository extends JpaRepository<ErpSyncLogEntity, Long> {
}
