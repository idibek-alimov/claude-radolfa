package tj.radolfa.application.services;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadProductForSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.ProductStatus;

/**
 * Resets PENDING_REVIEW / REJECTED products to DRAFT when any edit action fires.
 * Called as the first action in every UpdateProduct* service.
 */
@Component
public class ProductEditGuard {

    private final LoadProductBasePort      load;
    private final SaveProductHierarchyPort save;
    private final LoadProductForSkuPort    productForSku;
    private final LoadListingVariantPort   variantPort;

    public ProductEditGuard(LoadProductBasePort load,
                            SaveProductHierarchyPort save,
                            LoadProductForSkuPort productForSku,
                            LoadListingVariantPort variantPort) {
        this.load          = load;
        this.save          = save;
        this.productForSku = productForSku;
        this.variantPort   = variantPort;
    }

    public void resetIfNeeded(Long productBaseId) {
        ProductBase pb = load.findById(productBaseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productBaseId));
        ProductStatus before = pb.getStatus();
        pb.resetToDraftOnEdit();
        if (pb.getStatus() != before) {
            save.saveBase(pb);
        }
    }

    public void resetIfNeededBySkuId(Long skuId) {
        productForSku.findProductBaseIdBySkuId(skuId).ifPresent(this::resetIfNeeded);
    }

    public void resetIfNeededByVariantId(Long variantId) {
        variantPort.findVariantById(variantId)
                .map(ListingVariant::getProductBaseId)
                .ifPresent(this::resetIfNeeded);
    }
}
