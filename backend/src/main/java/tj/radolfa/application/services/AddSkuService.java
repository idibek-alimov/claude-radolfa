package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.AddSkuUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.Sku;

import java.util.UUID;

/**
 * Adds a new SKU (size variant) to an existing listing variant.
 */
@Service
public class AddSkuService implements AddSkuUseCase {

    private final LoadListingVariantPort loadVariantPort;
    private final SaveProductHierarchyPort savePort;

    public AddSkuService(LoadListingVariantPort loadVariantPort,
                         SaveProductHierarchyPort savePort) {
        this.loadVariantPort = loadVariantPort;
        this.savePort        = savePort;
    }

    @Override
    @Transactional
    public Result execute(Command command) {
        // Validate the variant exists
        loadVariantPort.findVariantById(command.variantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "ListingVariant not found: id=" + command.variantId()));

        Sku sku = new Sku(
                null,
                command.variantId(),
                "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                command.sizeLabel(),
                command.stockQuantity(),
                command.price()
        );

        Sku saved = savePort.saveSku(sku, command.variantId());
        return new Result(saved.getId(), saved.getSkuCode());
    }
}
