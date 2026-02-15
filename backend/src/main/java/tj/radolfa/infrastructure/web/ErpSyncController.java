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
import tj.radolfa.application.ports.in.SyncOrdersUseCase;
import tj.radolfa.application.ports.in.SyncOrdersUseCase.SyncOrderCommand;
import tj.radolfa.application.ports.in.SyncOrdersUseCase.SyncOrderItemCommand;
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
import tj.radolfa.infrastructure.web.dto.SyncOrderPayload;
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
    private final SyncOrdersUseCase          syncOrdersUseCase;
    private final LogSyncEventPort            logSyncEvent;

    public ErpSyncController(SyncProductHierarchyUseCase syncUseCase,
                             SyncCategoriesUseCase      syncCategoriesUseCase,
                             SyncLoyaltyPointsUseCase   loyaltyUseCase,
                             SyncOrdersUseCase          syncOrdersUseCase,
                             LogSyncEventPort            logSyncEvent) {
        this.syncUseCase           = syncUseCase;
        this.syncCategoriesUseCase = syncCategoriesUseCase;
        this.loyaltyUseCase        = loyaltyUseCase;
        this.syncOrdersUseCase     = syncOrdersUseCase;
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

        try {
            var command = new SyncCategoriesCommand(
                    payload.categories().stream()
                            .map(cp -> new SyncCategoriesCommand.CategoryPayload(cp.name(), cp.parentName()))
                            .toList()
            );

            List<CategoryView> result = syncCategoriesUseCase.execute(command);
            logSyncEvent.log("CATEGORIES", true, null);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            LOG.error("[ERP-SYNC] Failed to sync categories: {}", ex.getMessage(), ex);
            logSyncEvent.log("CATEGORIES", false, ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }
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

        try {
            loyaltyUseCase.execute(new SyncLoyaltyCommand(request.phone(), request.points()));
            logSyncEvent.log("LOYALTY:" + request.phone(), true, null);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            LOG.error("[LOYALTY-SYNC] Failed to sync points for phone={}: {}",
                    request.phone(), ex.getMessage(), ex);
            logSyncEvent.log("LOYALTY:" + request.phone(), false, ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Syncs an order from ERPNext. Upserts by erp_order_id.
     * Matches user by phone number. Skips if user not found.
     */
    @PostMapping("/orders")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<SyncResultDto> syncOrder(
            @Valid @RequestBody SyncOrderPayload payload) {

        LOG.info("[ORDER-SYNC] Received order sync for erpOrderId={}, phone={}",
                payload.erpOrderId(), payload.customerPhone());

        int synced = 0;
        int errors = 0;

        try {
            var items = payload.items().stream()
                    .map(ip -> new SyncOrderItemCommand(
                            ip.erpItemCode(), ip.productName(), ip.quantity(), ip.price()))
                    .toList();

            var command = new SyncOrderCommand(
                    payload.erpOrderId(),
                    payload.customerPhone(),
                    payload.status(),
                    payload.totalAmount(),
                    items);

            var result = syncOrdersUseCase.execute(command);

            if (result.status() == SyncOrdersUseCase.SyncStatus.SKIPPED) {
                LOG.warn("[ORDER-SYNC] Skipped: {}", result.message());
                logSyncEvent.log("ORDER:" + payload.erpOrderId(), false, result.message());
                return ResponseEntity.unprocessableEntity()
                        .body(new SyncResultDto(0, 0, result.message()));
            }

            logSyncEvent.log("ORDER:" + payload.erpOrderId(), true, null);
            synced = 1;
        } catch (Exception ex) {
            LOG.error("[ORDER-SYNC] Failed to sync order={}: {}",
                    payload.erpOrderId(), ex.getMessage(), ex);
            logSyncEvent.log("ORDER:" + payload.erpOrderId(), false, ex.getMessage());
            errors = 1;
        }

        return ResponseEntity.ok(new SyncResultDto(synced, errors, null));
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
