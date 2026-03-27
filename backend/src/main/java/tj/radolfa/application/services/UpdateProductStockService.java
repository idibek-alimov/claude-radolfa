package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.UpdateProductStockUseCase;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
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

    public UpdateProductStockService(LoadSkuPort loadSkuPort,
                                     SaveProductHierarchyPort savePort) {
        this.loadSkuPort = loadSkuPort;
        this.savePort    = savePort;
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
        Sku sku = requireSku(skuId);
        int current = sku.getStockQuantity() != null ? sku.getStockQuantity() : 0;
        int newStock = current - quantity;
        if (newStock < 0) {
            throw new IllegalStateException(
                    "Insufficient stock for SKU id=" + skuId +
                    " (current=" + current + ", requested=" + quantity + ")");
        }
        sku.updatePriceAndStock(sku.getPrice(), newStock);
        savePort.saveSku(sku, sku.getListingVariantId());
        LOG.debug("[STOCK] SKU id={} decremented by {} → {}", skuId, quantity, newStock);
    }

    @Override
    @Transactional
    public void increment(Long skuId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        Sku sku = requireSku(skuId);
        int current = sku.getStockQuantity() != null ? sku.getStockQuantity() : 0;
        int newStock = current + quantity;
        sku.updatePriceAndStock(sku.getPrice(), newStock);
        savePort.saveSku(sku, sku.getListingVariantId());
        LOG.debug("[STOCK] SKU id={} incremented by {} → {}", skuId, quantity, newStock);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Sku requireSku(Long skuId) {
        return loadSkuPort.findSkuById(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: id=" + skuId));
    }
}
