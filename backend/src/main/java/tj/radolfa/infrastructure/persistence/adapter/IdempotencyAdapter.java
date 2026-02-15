package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.IdempotencyPort;
import tj.radolfa.infrastructure.persistence.entity.IdempotencyRecordEntity;
import tj.radolfa.infrastructure.persistence.repository.IdempotencyRecordRepository;

/**
 * Hexagonal adapter that checks and persists idempotency keys
 * into the {@code erp_sync_idempotency} table via Spring Data.
 */
@Component
public class IdempotencyAdapter implements IdempotencyPort {

    private final IdempotencyRecordRepository repository;

    public IdempotencyAdapter(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean exists(String key, String eventType) {
        return repository.existsByIdempotencyKeyAndEventType(key, eventType);
    }

    @Override
    public void save(String key, String eventType, int responseStatus) {
        var entity = new IdempotencyRecordEntity();
        entity.setIdempotencyKey(key);
        entity.setEventType(eventType);
        entity.setResponseStatus(responseStatus);
        repository.save(entity);
    }
}
