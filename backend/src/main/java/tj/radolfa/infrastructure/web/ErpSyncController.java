package tj.radolfa.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import tj.radolfa.application.ports.in.SyncErpProductUseCase;
import tj.radolfa.application.ports.out.LogSyncEventPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.infrastructure.erp.ErpProductSnapshot;
import tj.radolfa.infrastructure.web.dto.SyncResultDto;

import java.util.List;

/**
 * REST adapter for incremental (push-based) ERP sync.
 *
 * <p>ERPNext (or a webhook bridge) POSTs a JSON array of product snapshots.
 * Each snapshot is forwarded to {@link SyncErpProductUseCase} -- the single
 * authorised upsert path. Individual failures are logged and counted but
 * do not abort the remaining items.
 *
 * <h3>Security</h3>
 * <p>This endpoint is protected by Spring Security. Only users with the
 * {@code SYSTEM} role can access it. The role is verified via JWT token
 * in the Authorization header.
 *
 * <h3>Critical Constraint</h3>
 * <p>ERPNext is the SOURCE OF TRUTH for price, name, stock. Only SYSTEM
 * role can modify these fields through this sync endpoint.
 */
@RestController
@RequestMapping("/api/v1/sync")
public class ErpSyncController {

    private static final Logger LOG = LoggerFactory.getLogger(ErpSyncController.class);

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
     * <p>Requires SYSTEM role (enforced by Spring Security filter chain
     * and method-level security annotation).
     *
     * @param snapshots the array of product snapshots to sync
     * @return 200 with {@link SyncResultDto}, or 403 if unauthorized
     */
    @PostMapping("/products")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<SyncResultDto> syncProducts(@RequestBody List<ErpProductSnapshot> snapshots) {

        LOG.info("[ERP-SYNC] Received sync request with {} products", snapshots.size());

        int synced = 0;
        int errors = 0;

        for (ErpProductSnapshot snapshot : snapshots) {
            try {
                syncUseCase.execute(
                        snapshot.erpId(),
                        snapshot.name(),
                        Money.of(snapshot.price()),
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

        LOG.info("[ERP-SYNC] Completed -- synced={}, errors={}", synced, errors);
        return ResponseEntity.ok(new SyncResultDto(synced, errors));
    }
}
