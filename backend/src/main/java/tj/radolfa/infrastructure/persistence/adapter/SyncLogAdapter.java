package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LogImportEventPort;
import tj.radolfa.infrastructure.persistence.entity.SyncLogEntity;
import tj.radolfa.infrastructure.persistence.repository.SyncLogRepository;

import java.time.Instant;

/**
 * Hexagonal adapter that persists import-event audit rows
 * into the {@code sync_log} table via Spring Data.
 */
@Component
public class SyncLogAdapter implements LogImportEventPort {

    private final SyncLogRepository repository;

    public SyncLogAdapter(SyncLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void log(String importId, boolean success, String errorMessage) {
        SyncLogEntity entity = new SyncLogEntity(
                null,                          // id – generated
                importId,
                Instant.now(),                 // synced_at
                success ? "SUCCESS" : "ERROR",
                success ? null : errorMessage
        );
        repository.save(entity);
    }
}
