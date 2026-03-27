package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.UpdateProductPriceUseCase;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Sku;

/**
 * Sets the price on a specific SKU.
 * ADMIN only — enforced at the controller level.
 */
@Service
public class UpdateProductPriceService implements UpdateProductPriceUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateProductPriceService.class);

    private final LoadSkuPort              loadSkuPort;
    private final SaveProductHierarchyPort savePort;

    public UpdateProductPriceService(LoadSkuPort loadSkuPort,
                                     SaveProductHierarchyPort savePort) {
        this.loadSkuPort = loadSkuPort;
        this.savePort    = savePort;
    }

    @Override
    @Transactional
    public void execute(Long skuId, Money newPrice) {
        Sku sku = loadSkuPort.findSkuById(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: id=" + skuId));

        sku.updatePriceAndStock(newPrice, sku.getStockQuantity());
        savePort.saveSku(sku, sku.getListingVariantId());

        LOG.info("[UPDATE-PRICE] SKU id={} price updated to {}", skuId, newPrice.amount());
    }
}
