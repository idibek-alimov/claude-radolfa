package tj.radolfa.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tj.radolfa.application.ports.in.SyncErpProductUseCase;
import tj.radolfa.application.ports.out.LogSyncEventPort;
import tj.radolfa.domain.exception.ErpLockViolationException;
import tj.radolfa.infrastructure.erp.ErpProductSnapshot;
import tj.radolfa.infrastructure.web.dto.SyncResultDto;

import java.util.List;

/**
 * REST adapter for incremental (push-based) ERP sync.
 *
 * ERPNext (or a webhook bridge) POSTs a JSON array of product snapshots.
 * Each snapshot is forwarded to {@link SyncErpProductUseCase} – the single
 * authorised upsert path.  Individual failures are logged and counted but
 * do not abort the remaining items.
 *
 * <h3>Security (placeholder)</h3>
 * Until the full auth system is wired in Phase 5, callers must send the
 * header {@code X-Sync-Role: SYSTEM}.  Any other value (or its absence)
 * results in a {@code 403 Forbidden}.
 */
@RestController
@RequestMapping("/api/v1/sync")
public class ErpSyncController {

    private static final Logger LOG = LoggerFactory.getLogger(ErpSyncController.class);

    private static final String SYNC_ROLE_HEADER   = "X-Sync-Role";
    private static final String REQUIRED_ROLE      = "SYSTEM";

    private final SyncErpProductUseCase syncUseCase;
    private final LogSyncEventPort      logSyncEvent;

    public ErpSyncController(SyncErpProductUseCase syncUseCase,
                             LogSyncEventPort      logSyncEvent) {
        this.syncUseCase  = syncUseCase;
        this.logSyncEvent = logSyncEvent;
    }

    // ----------------------------------------------------------------
    // Endpoint
    // ----------------------------------------------------------------

    /**
     * Accepts a JSON array of ERP product snapshots, syncs each one,
     * and returns a summary of the operation.
     *
     * @param role      value of the {@code X-Sync-Role} request header
     * @param snapshots the array of product snapshots to sync
     * @return 200 with {@link SyncResultDto}, or 403 if role is wrong
     */
    @PostMapping("/products")
    public ResponseEntity<SyncResultDto> syncProducts(
            @RequestHeader(value = SYNC_ROLE_HEADER, defaultValue = "") String role,
            @RequestBody List<ErpProductSnapshot> snapshots) {

        guardSystemRole(role);

        int synced = 0;
        int errors = 0;

        for (ErpProductSnapshot snapshot : snapshots) {
            try {
                syncUseCase.execute(
                        snapshot.erpId(),
                        snapshot.name(),
                        snapshot.price(),
                        snapshot.stock()
                );
                logSyncEvent.log(snapshot.erpId(), true, null);
                synced++;
            } catch (Exception ex) {
                LOG.error("[ERP-SYNC] Failed to sync erpId={}: {}", snapshot.erpId(), ex.getMessage(), ex);
                logSyncEvent.log(snapshot.erpId(), false, ex.getMessage());
                errors++;
            }
        }

        LOG.info("[ERP-SYNC] Completed – synced={}, errors={}", synced, errors);
        return ResponseEntity.ok(new SyncResultDto(synced, errors));
    }

    // ----------------------------------------------------------------
    // Guard – placeholder for Phase 5 auth
    // ----------------------------------------------------------------

    /**
     * Throws {@link ErpLockViolationException} (mapped to 403) when the
     * caller has not declared the {@code SYSTEM} role via the header.
     */
    private void guardSystemRole(String role) {
        if (!REQUIRED_ROLE.equals(role)) {
            throw new ErpLockViolationException("sync endpoint");
        }
    }

    // ----------------------------------------------------------------
    // Exception mapping (scoped to this controller only)
    // ----------------------------------------------------------------

    @ExceptionHandler(ErpLockViolationException.class)
    public ResponseEntity<String> handleLockViolation(ErpLockViolationException ex) {
        LOG.warn("[ERP-SYNC] Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
