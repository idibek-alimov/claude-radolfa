package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LogSyncEventPort;
import tj.radolfa.infrastructure.persistence.entity.ErpSyncLogEntity;
import tj.radolfa.infrastructure.persistence.repository.ErpSyncLogRepository;

import java.time.Instant;

/**
 * Hexagonal adapter that persists sync-event audit rows
 * into the {@code erp_sync_log} table via Spring Data.
 */
@Component
public class ErpSyncLogAdapter implements LogSyncEventPort {

    private final ErpSyncLogRepository repository;

    public ErpSyncLogAdapter(ErpSyncLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void log(String erpId, boolean success, String errorMessage) {
        ErpSyncLogEntity entity = new ErpSyncLogEntity(
                null,                          // id – generated
                erpId,
                Instant.now(),                 // synced_at
                success ? "SUCCESS" : "ERROR",
                success ? null : errorMessage
        );
        repository.save(entity);
    }
}
