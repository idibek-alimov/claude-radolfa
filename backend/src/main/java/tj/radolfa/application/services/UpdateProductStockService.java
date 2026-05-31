package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.SkuEditActor;
import tj.radolfa.application.ports.in.product.UpdateProductStockUseCase;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.ports.out.LoadSkuOwnerPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.application.ports.out.RecordInventoryTransactionPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.exception.InsufficientStockException;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.service.SkuFieldOwnershipGuard;

import java.time.Instant;

/**
 * Manages stock quantities. All mutations route through InventoryPlacementPort so that
 * inventory_placements is the authoritative source; skus.stock_quantity is a
 * transactionally-recomputed mirror maintained by the adapter.
 *
 * <p>Implements both {@link UpdateProductStockUseCase} (user-facing: ADMIN / SELLER API use,
 * ownership-guarded) and {@link StockAdjustmentPort} (internal system use by checkout and
 * cancellation — <strong>no ownership guard</strong>). Every successful stock change also
 * writes a ledger row via {@link RecordInventoryTransactionPort} in the same transaction.
 * A denied edit (FieldLockException) never reaches the ledger write.
 */
@Service
public class UpdateProductStockService implements UpdateProductStockUseCase, StockAdjustmentPort {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateProductStockService.class);

    private final LoadSkuPort                    loadSkuPort;
    private final LoadSkuOwnerPort               loadSkuOwnerPort;
    private final InventoryPlacementPort         placementPort;
    private final RecordInventoryTransactionPort recordInventoryTransactionPort;
    private final LoadWarehousePort              loadWarehousePort;
    private final ProductEditGuard               editGuard;

    public UpdateProductStockService(LoadSkuPort loadSkuPort,
                                     LoadSkuOwnerPort loadSkuOwnerPort,
                                     InventoryPlacementPort placementPort,
                                     RecordInventoryTransactionPort recordInventoryTransactionPort,
                                     LoadWarehousePort loadWarehousePort,
                                     ProductEditGuard editGuard) {
        this.loadSkuPort                    = loadSkuPort;
        this.loadSkuOwnerPort               = loadSkuOwnerPort;
        this.placementPort                  = placementPort;
        this.recordInventoryTransactionPort = recordInventoryTransactionPort;
        this.loadWarehousePort              = loadWarehousePort;
        this.editGuard                      = editGuard;
    }

    // ── UpdateProductStockUseCase (ownership-guarded user-facing API) ─────────

    @Override
    @Transactional
    public void setAbsolute(Long skuId, int quantity, SkuEditActor actor) {
        assertOwnership(skuId, actor);
        doSetAbsolute(skuId, quantity, actor.userId());
    }

    @Override
    @Transactional
    public void adjust(Long skuId, int delta, SkuEditActor actor) {
        assertOwnership(skuId, actor);
        doAdjust(skuId, delta, actor.userId());
    }

    // ── StockAdjustmentPort (internal system paths — no ownership guard) ──────

    @Override
    @Transactional
    public void setAbsolute(Long skuId, int quantity) {
        // System path (e.g. stock receipt) — bypasses guard intentionally
        doSetAbsolute(skuId, quantity, null);
    }

    @Override
    @Transactional
    public void decrement(Long skuId, int quantity) {
        decrement(skuId, quantity, null, null);
    }

    @Override
    @Transactional
    public void increment(Long skuId, int quantity) {
        increment(skuId, quantity, InventoryTransactionType.CANCELLATION, "ORDER", null, null);
    }

    // ── StockAdjustmentPort (context-aware overloads) ─────────────────────────

    @Override
    @Transactional
    public void decrement(Long skuId, int quantity, Long orderId, Long actorUserId) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        Long warehouseId = loadWarehousePort.findDefault().id();
        boolean ok = placementPort.decrementForSale(skuId, warehouseId, quantity);
        if (!ok) {
            throw new InsufficientStockException(skuId, placementPort.totalForSku(skuId, warehouseId), quantity);
        }
        recordInventoryTransactionPort.record(new InventoryTransaction(
                null, skuId, warehouseId, -quantity, InventoryTransactionType.SALE,
                "ORDER", orderId, actorUserId, null, Instant.now()));
        LOG.debug("[STOCK] SKU id={} decremented by {} for orderId={}", skuId, quantity, orderId);
    }

    @Override
    @Transactional
    public void increment(Long skuId, int quantity, InventoryTransactionType type,
                          String referenceType, Long referenceId, Long actorUserId) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        requireSku(skuId);
        Long warehouseId = loadWarehousePort.findDefault().id();
        placementPort.addToInbound(skuId, warehouseId, quantity);
        recordInventoryTransactionPort.record(new InventoryTransaction(
                null, skuId, warehouseId, quantity, type,
                referenceType, referenceId, actorUserId, null, Instant.now()));
        LOG.debug("[STOCK] SKU id={} incremented by {} type={}", skuId, quantity, type);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /** Checks ownership before any mutation. Throws FieldLockException if denied. */
    private void assertOwnership(Long skuId, SkuEditActor actor) {
        LoadSkuOwnerPort.SkuOwner owner = loadSkuOwnerPort.findBySkuId(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: id=" + skuId));
        SkuFieldOwnershipGuard.assertCanEditPriceOrStock(actor.role(), actor.sellerId(), owner.sellerId());
    }

    /** Core set-absolute logic — no ownership guard (shared by user-path and system-path). */
    private void doSetAbsolute(Long skuId, int quantity, Long actorUserId) {
        editGuard.resetIfNeededBySkuId(skuId);
        if (quantity < 0) throw new IllegalArgumentException("quantity must be ≥ 0");
        Long warehouseId = loadWarehousePort.findDefault().id();
        int current = placementPort.totalForSku(skuId, warehouseId);
        int delta = quantity - current;
        if (delta != 0) {
            placementPort.adjustInbound(skuId, warehouseId, delta);
            recordInventoryTransactionPort.record(new InventoryTransaction(
                    null, skuId, warehouseId, delta, InventoryTransactionType.MANUAL_ADJUSTMENT,
                    "MANUAL", null, actorUserId, null, Instant.now()));
        }
        LOG.info("[STOCK] SKU id={} set to {} by actorUserId={}", skuId, quantity, actorUserId);
    }

    /** Core adjust logic — no ownership guard (shared by user-path and system-path). */
    private void doAdjust(Long skuId, int delta, Long actorUserId) {
        if (delta == 0) return;
        editGuard.resetIfNeededBySkuId(skuId);
        Long warehouseId = loadWarehousePort.findDefault().id();
        if (delta < 0) {
            int quantity = -delta;
            boolean ok = placementPort.decrementForSale(skuId, warehouseId, quantity);
            if (!ok) {
                throw new InsufficientStockException(skuId, placementPort.totalForSku(skuId, warehouseId), quantity);
            }
            recordInventoryTransactionPort.record(new InventoryTransaction(
                    null, skuId, warehouseId, delta, InventoryTransactionType.MANUAL_ADJUSTMENT,
                    "MANUAL", null, actorUserId, null, Instant.now()));
        } else {
            increment(skuId, delta, InventoryTransactionType.MANUAL_ADJUSTMENT,
                    "MANUAL", null, actorUserId);
        }
        LOG.debug("[STOCK] SKU id={} adjusted by {} actorUserId={}", skuId, delta, actorUserId);
    }

    private void requireSku(Long skuId) {
        loadSkuPort.findSkuById(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: id=" + skuId));
    }
}
