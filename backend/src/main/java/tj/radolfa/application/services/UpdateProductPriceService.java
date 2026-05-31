package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.SkuEditActor;
import tj.radolfa.application.ports.in.product.UpdateProductPriceUseCase;
import tj.radolfa.application.ports.out.LoadSkuOwnerPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.service.SkuFieldOwnershipGuard;

/**
 * Sets the price on a specific SKU.
 * Ownership is enforced at the service layer: ADMIN may update any SKU;
 * a SELLER may only update SKUs of products they own; Radolfa-owned
 * products remain ADMIN-only. Violations throw {@code FieldLockException} (→ 403).
 */
@Service
public class UpdateProductPriceService implements UpdateProductPriceUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateProductPriceService.class);

    private final LoadSkuPort              loadSkuPort;
    private final LoadSkuOwnerPort         loadSkuOwnerPort;
    private final SaveProductHierarchyPort savePort;
    private final ProductEditGuard         editGuard;

    public UpdateProductPriceService(LoadSkuPort loadSkuPort,
                                     LoadSkuOwnerPort loadSkuOwnerPort,
                                     SaveProductHierarchyPort savePort,
                                     ProductEditGuard editGuard) {
        this.loadSkuPort      = loadSkuPort;
        this.loadSkuOwnerPort = loadSkuOwnerPort;
        this.savePort         = savePort;
        this.editGuard        = editGuard;
    }

    @Override
    @Transactional
    public void execute(Long skuId, Money newPrice, SkuEditActor actor) {
        // Resolve ownership first — distinguishes "not found" from "Radolfa-owned (null)"
        LoadSkuOwnerPort.SkuOwner owner = loadSkuOwnerPort.findBySkuId(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: id=" + skuId));

        // Guard runs before any mutation — a denied edit changes nothing
        SkuFieldOwnershipGuard.assertCanEditPriceOrStock(actor.role(), actor.sellerId(), owner.sellerId());

        editGuard.resetIfNeededBySkuId(skuId);
        Sku sku = loadSkuPort.findSkuById(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: id=" + skuId));

        sku.updatePriceAndStock(newPrice, sku.getStockQuantity());
        savePort.saveSku(sku, sku.getListingVariantId());

        LOG.info("[UPDATE-PRICE] SKU id={} price updated to {} by userId={}", skuId, newPrice.amount(), actor.userId());
    }
}
