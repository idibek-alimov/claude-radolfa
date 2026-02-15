package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.IdempotencyRecordEntity;

/**
 * Spring Data JPA repository for {@link IdempotencyRecordEntity}.
 */
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, Long> {

    boolean existsByIdempotencyKeyAndEventType(String idempotencyKey, String eventType);
}
