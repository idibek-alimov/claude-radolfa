package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.AddSkuToVariantUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Sku;

import java.util.UUID;

/**
 * Adds a new SKU to an existing listing variant.
 * ADMIN only — enforced at the controller level (price + stock are ADMIN-locked fields).
 */
@Service
public class AddSkuToVariantService implements AddSkuToVariantUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(AddSkuToVariantService.class);

    private final LoadListingVariantPort    loadVariantPort;
    private final SaveProductHierarchyPort  savePort;

    public AddSkuToVariantService(LoadListingVariantPort loadVariantPort,
                                  SaveProductHierarchyPort savePort) {
        this.loadVariantPort = loadVariantPort;
        this.savePort        = savePort;
    }

    @Override
    @Transactional
    public Long execute(Command command) {
        ListingVariant variant = loadVariantPort.findVariantById(command.variantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ListingVariant not found: id=" + command.variantId()));

        if (!command.productBaseId().equals(variant.getProductBaseId())) {
            throw new ResourceNotFoundException(
                    "ListingVariant " + command.variantId()
                    + " does not belong to product " + command.productBaseId());
        }

        String skuCode = "SKU-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String barcode = "BC-"  + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        Sku sku = new Sku(
                null,
                command.variantId(),
                skuCode,
                command.sizeLabel(),
                command.stockQuantity(),
                command.price(),
                barcode);

        Sku saved = savePort.saveSku(sku, command.variantId());
        LOG.info("[ADD-SKU] variantId={} skuId={} sizeLabel='{}' price={}",
                command.variantId(), saved.getId(), command.sizeLabel(),
                command.price() != null ? command.price().amount() : null);

        return saved.getId();
    }
}
