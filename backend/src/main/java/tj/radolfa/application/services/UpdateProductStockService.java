package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.UpdateProductStockUseCase;
import tj.radolfa.application.ports.out.AtomicStockPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.RecordInventoryTransactionPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.exception.InsufficientStockException;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.Sku;

import java.time.Instant;

/**
 * Manages stock quantities on SKUs.
 *
 * <p>Implements both {@link UpdateProductStockUseCase} (for ADMIN/API use)
 * and {@link StockAdjustmentPort} (for internal use by checkout and cancellation).
 * Every successful stock change also writes an immutable ledger row via
 * {@link RecordInventoryTransactionPort} within the same transaction.
 */
@Service
public class UpdateProductStockService implements UpdateProductStockUseCase, StockAdjustmentPort {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateProductStockService.class);

    private final LoadSkuPort                     loadSkuPort;
    private final SaveProductHierarchyPort        savePort;
    private final AtomicStockPort                 atomicStockPort;
    private final RecordInventoryTransactionPort  recordInventoryTransactionPort;

    public UpdateProductStockService(LoadSkuPort loadSkuPort,
                                     SaveProductHierarchyPort savePort,
                                     AtomicStockPort atomicStockPort,
                                     RecordInventoryTransactionPort recordInventoryTransactionPort) {
        this.loadSkuPort                    = loadSkuPort;
        this.savePort                       = savePort;
        this.atomicStockPort                = atomicStockPort;
        this.recordInventoryTransactionPort = recordInventoryTransactionPort;
    }

    // ── UpdateProductStockUseCase ──────────────────────────────────────────────

    @Override
    @Transactional
    public void setAbsolute(Long skuId, int quantity, Long actorUserId) {
        if (quantity < 0) throw new IllegalArgumentException("quantity must be ≥ 0");
        Sku sku = requireSku(skuId);
        int delta = quantity - (sku.getStockQuantity() != null ? sku.getStockQuantity() : 0);
        sku.updatePriceAndStock(sku.getPrice(), quantity);
        savePort.saveSku(sku, sku.getListingVariantId());
        if (delta != 0) {
            recordInventoryTransactionPort.record(new InventoryTransaction(
                    null, skuId, delta, InventoryTransactionType.MANUAL_ADJUSTMENT,
                    "MANUAL", null, actorUserId, null, Instant.now()));
        }
        LOG.info("[STOCK] SKU id={} set to {} by actorUserId={}", skuId, quantity, actorUserId);
    }

    @Override
    @Transactional
    public void adjust(Long skuId, int delta, Long actorUserId) {
        if (delta == 0) return;
        if (delta < 0) {
            int quantity = -delta;
            int updated = atomicStockPort.decrementIfAvailable(skuId, quantity);
            if (updated == 0) {
                Sku sku = requireSku(skuId);
                int available = sku.getStockQuantity() != null ? sku.getStockQuantity() : 0;
                throw new InsufficientStockException(skuId, available, quantity);
            }
            recordInventoryTransactionPort.record(new InventoryTransaction(
                    null, skuId, delta, InventoryTransactionType.MANUAL_ADJUSTMENT,
                    "MANUAL", null, actorUserId, null, Instant.now()));
        } else {
            increment(skuId, delta, InventoryTransactionType.MANUAL_ADJUSTMENT,
                    "MANUAL", null, actorUserId);
        }
        LOG.debug("[STOCK] SKU id={} adjusted by {} actorUserId={}", skuId, delta, actorUserId);
    }

    // ── StockAdjustmentPort (legacy — backward-compatible) ────────────────────

    @Override
    @Transactional
    public void setAbsolute(Long skuId, int quantity) {
        setAbsolute(skuId, quantity, null);
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
        int updated = atomicStockPort.decrementIfAvailable(skuId, quantity);
        if (updated == 0) {
            Sku sku = requireSku(skuId);
            int available = sku.getStockQuantity() != null ? sku.getStockQuantity() : 0;
            throw new InsufficientStockException(skuId, available, quantity);
        }
        recordInventoryTransactionPort.record(new InventoryTransaction(
                null, skuId, -quantity, InventoryTransactionType.SALE,
                "ORDER", orderId, actorUserId, null, Instant.now()));
        LOG.debug("[STOCK] SKU id={} decremented by {} for orderId={}", skuId, quantity, orderId);
    }

    @Override
    @Transactional
    public void increment(Long skuId, int quantity, InventoryTransactionType type,
                          String referenceType, Long referenceId, Long actorUserId) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        int updated = atomicStockPort.increment(skuId, quantity);
        if (updated == 0) {
            throw new IllegalArgumentException("SKU not found: id=" + skuId);
        }
        recordInventoryTransactionPort.record(new InventoryTransaction(
                null, skuId, quantity, type,
                referenceType, referenceId, actorUserId, null, Instant.now()));
        LOG.debug("[STOCK] SKU id={} incremented by {} type={}", skuId, quantity, type);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Sku requireSku(Long skuId) {
        return loadSkuPort.findSkuById(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: id=" + skuId));
    }
}
