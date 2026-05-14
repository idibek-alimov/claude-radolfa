package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.UpdateProductStockUseCase;
import tj.radolfa.application.ports.out.AtomicStockPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.exception.InsufficientStockException;
import tj.radolfa.domain.model.Sku;

/**
 * Manages stock quantities on SKUs.
 *
 * <p>Implements both {@link UpdateProductStockUseCase} (for ADMIN/API use)
 * and {@link StockAdjustmentPort} (for internal use by checkout and cancellation).
 */
@Service
public class UpdateProductStockService implements UpdateProductStockUseCase, StockAdjustmentPort {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateProductStockService.class);

    private final LoadSkuPort              loadSkuPort;
    private final SaveProductHierarchyPort savePort;
    private final AtomicStockPort          atomicStockPort;

    public UpdateProductStockService(LoadSkuPort loadSkuPort,
                                     SaveProductHierarchyPort savePort,
                                     AtomicStockPort atomicStockPort) {
        this.loadSkuPort     = loadSkuPort;
        this.savePort        = savePort;
        this.atomicStockPort = atomicStockPort;
    }

    // ── UpdateProductStockUseCase ──────────────────────────────────────────────

    @Override
    @Transactional
    public void setAbsolute(Long skuId, int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("quantity must be ≥ 0");
        Sku sku = requireSku(skuId);
        sku.updatePriceAndStock(sku.getPrice(), quantity);
        savePort.saveSku(sku, sku.getListingVariantId());
        LOG.info("[STOCK] SKU id={} set to {}", skuId, quantity);
    }

    @Override
    @Transactional
    public void adjust(Long skuId, int delta) {
        if (delta == 0) return;
        if (delta < 0) decrement(skuId, -delta);
        else           increment(skuId, delta);
    }

    // ── StockAdjustmentPort ───────────────────────────────────────────────────

    @Override
    @Transactional
    public void decrement(Long skuId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        int updated = atomicStockPort.decrementIfAvailable(skuId, quantity);
        if (updated == 0) {
            Sku sku = requireSku(skuId);
            int available = sku.getStockQuantity() != null ? sku.getStockQuantity() : 0;
            throw new InsufficientStockException(skuId, available, quantity);
        }
        LOG.debug("[STOCK] SKU id={} decremented by {}", skuId, quantity);
    }

    @Override
    @Transactional
    public void increment(Long skuId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        int updated = atomicStockPort.increment(skuId, quantity);
        if (updated == 0) {
            throw new IllegalArgumentException("SKU not found: id=" + skuId);
        }
        LOG.debug("[STOCK] SKU id={} incremented by {}", skuId, quantity);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Sku requireSku(Long skuId) {
        return loadSkuPort.findSkuById(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: id=" + skuId));
    }
}
