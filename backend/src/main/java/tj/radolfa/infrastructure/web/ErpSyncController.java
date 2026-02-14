package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import tj.radolfa.application.ports.in.SyncCategoriesUseCase;
import tj.radolfa.application.ports.in.SyncCategoriesUseCase.SyncCategoriesCommand;
import tj.radolfa.application.ports.in.SyncLoyaltyPointsUseCase;
import tj.radolfa.application.ports.in.SyncLoyaltyPointsUseCase.SyncLoyaltyCommand;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand.VariantCommand;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand.SkuCommand;
import tj.radolfa.application.ports.out.LogSyncEventPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.application.ports.out.LoadCategoryPort.CategoryView;
import tj.radolfa.infrastructure.web.dto.ErpHierarchyPayload;
import tj.radolfa.infrastructure.web.dto.SyncCategoriesPayload;
import tj.radolfa.infrastructure.web.dto.SyncLoyaltyRequestDto;
import tj.radolfa.infrastructure.web.dto.SyncResultDto;

import java.util.List;

/**
 * REST adapter for ERP product synchronisation.
 *
 * <p>Accepts a rich hierarchy payload from ERPNext:
 * Template → Variant (Colour) → Item (Size/SKU).
 *
 * <h3>Security</h3>
 * Only users with the {@code SYSTEM} role can access this endpoint.
 *
 * <h3>Critical Constraint</h3>
 * ERPNext is the SOURCE OF TRUTH for price, name, stock.
 */
@RestController
@RequestMapping("/api/v1/sync")
public class ErpSyncController {

    private static final Logger LOG = LoggerFactory.getLogger(ErpSyncController.class);

    private final SyncProductHierarchyUseCase syncUseCase;
    private final SyncCategoriesUseCase      syncCategoriesUseCase;
    private final SyncLoyaltyPointsUseCase   loyaltyUseCase;
    private final LogSyncEventPort            logSyncEvent;

    public ErpSyncController(SyncProductHierarchyUseCase syncUseCase,
                             SyncCategoriesUseCase      syncCategoriesUseCase,
                             SyncLoyaltyPointsUseCase   loyaltyUseCase,
                             LogSyncEventPort            logSyncEvent) {
        this.syncUseCase           = syncUseCase;
        this.syncCategoriesUseCase = syncCategoriesUseCase;
        this.loyaltyUseCase        = loyaltyUseCase;
        this.logSyncEvent          = logSyncEvent;
    }

    /**
     * Accepts a hierarchy payload, performs idempotent upsert
     * across ProductBase → ListingVariant → Sku, and returns
     * a summary of the operation.
     */
    @PostMapping("/products")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<SyncResultDto> syncProducts(
            @Valid @RequestBody ErpHierarchyPayload payload) {

        LOG.info("[ERP-SYNC] Received hierarchy sync for template={}",
                payload.templateCode());

        int synced = 0;
        int errors = 0;

        try {
            HierarchySyncCommand command = toCommand(payload);
            syncUseCase.execute(command);
            logSyncEvent.log(payload.templateCode(), true, null);
            synced = 1;
        } catch (Exception ex) {
            LOG.error("[ERP-SYNC] Failed to sync template={}: {}",
                    payload.templateCode(), ex.getMessage(), ex);
            logSyncEvent.log(payload.templateCode(), false, ex.getMessage());
            errors = 1;
        }

        LOG.info("[ERP-SYNC] Completed -- synced={}, errors={}", synced, errors);
        return ResponseEntity.ok(new SyncResultDto(synced, errors));
    }

    /**
     * Syncs the category hierarchy from ERPNext.
     * Must be called BEFORE product sync to ensure categories exist.
     */
    @PostMapping("/categories")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<List<CategoryView>> syncCategories(
            @Valid @RequestBody SyncCategoriesPayload payload) {

        LOG.info("[ERP-SYNC] Received category sync with {} categories",
                payload.categories().size());

        var command = new SyncCategoriesCommand(
                payload.categories().stream()
                        .map(cp -> new SyncCategoriesCommand.CategoryPayload(cp.name(), cp.parentName()))
                        .toList()
        );

        List<CategoryView> result = syncCategoriesUseCase.execute(command);
        return ResponseEntity.ok(result);
    }

    /**
     * Syncs loyalty points for a user, looked up by phone number.
     * If the user doesn't exist, a warning is logged and 204 is returned.
     */
    @PostMapping("/loyalty")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<Void> syncLoyaltyPoints(
            @Valid @RequestBody SyncLoyaltyRequestDto request) {

        LOG.info("[LOYALTY-SYNC] Received points sync for phone={}", request.phone());
        loyaltyUseCase.execute(new SyncLoyaltyCommand(request.phone(), request.points()));
        return ResponseEntity.noContent().build();
    }

    // ---- Mapping: DTO → Command ----

    private HierarchySyncCommand toCommand(ErpHierarchyPayload payload) {
        var variants = payload.variants().stream()
                .map(this::toVariantCommand)
                .toList();

        return new HierarchySyncCommand(
                payload.templateCode(),
                payload.templateName(),
                payload.category(),
                variants
        );
    }

    private VariantCommand toVariantCommand(ErpHierarchyPayload.VariantPayload vp) {
        var items = vp.items().stream()
                .map(this::toSkuCommand)
                .toList();

        return new VariantCommand(vp.colorKey(), items);
    }

    private SkuCommand toSkuCommand(ErpHierarchyPayload.ItemPayload ip) {
        return new SkuCommand(
                ip.erpItemCode(),
                ip.sizeLabel(),
                ip.stockQuantity(),
                Money.of(ip.price().list()),
                Money.of(ip.price().effective()),
                ip.price().saleEndsAt()
        );
    }
}
