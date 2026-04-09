package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.AddVariantToProductUseCase;
import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.LoadColorPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.DuplicateResourceException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;

import java.util.Collections;
import java.util.List;

/**
 * Adds a new (empty) color variant to an existing ProductBase.
 * MANAGER or ADMIN — enforced at the controller level.
 */
@Service
public class AddVariantToProductService implements AddVariantToProductUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(AddVariantToProductService.class);

    private final LoadProductBasePort       loadBasePort;
    private final LoadColorPort             loadColorPort;
    private final LoadListingVariantPort    loadVariantPort;
    private final SaveProductHierarchyPort  savePort;
    private final ListingIndexPort          listingIndexPort;

    public AddVariantToProductService(LoadProductBasePort loadBasePort,
                                      LoadColorPort loadColorPort,
                                      LoadListingVariantPort loadVariantPort,
                                      SaveProductHierarchyPort savePort,
                                      ListingIndexPort listingIndexPort) {
        this.loadBasePort     = loadBasePort;
        this.loadColorPort    = loadColorPort;
        this.loadVariantPort  = loadVariantPort;
        this.savePort         = savePort;
        this.listingIndexPort = listingIndexPort;
    }

    @Override
    @Transactional
    public Result execute(Command command) {
        ProductBase base = loadBasePort.findById(command.productBaseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProductBase not found: id=" + command.productBaseId()));

        LoadColorPort.ColorView color = loadColorPort.findById(command.colorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Color not found: id=" + command.colorId()));

        loadVariantPort.findByProductBaseIdAndColorKey(command.productBaseId(), color.colorKey())
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Variant with color '" + color.colorKey()
                            + "' already exists for productBaseId=" + command.productBaseId());
                });

        ListingVariant variant = new ListingVariant(
                null,
                command.productBaseId(),
                color.colorKey(),
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null,
                false,   // isEnabled — disabled until content is filled in
                true,    // isActive
                null, null, null, null
        );
        variant.generateSlug(base.getExternalRef());

        ListingVariant saved = savePort.saveVariant(variant, command.productBaseId());

        LOG.info("[ADD-VARIANT] productBaseId={} variantId={} slug='{}' colorKey='{}'",
                command.productBaseId(), saved.getId(), saved.getSlug(), color.colorKey());

        // ES indexing — fire-and-forget, outside transaction boundary by design
        try {
            listingIndexPort.index(
                    saved.getId(),
                    saved.getSlug(),
                    base.getName(),
                    base.getCategory(),
                    color.colorKey(),
                    color.hexCode(),
                    null,
                    List.of(),
                    null,
                    0,
                    null,
                    saved.getProductCode(),
                    List.of());
        } catch (Exception ex) {
            LOG.error("[ADD-VARIANT] ES indexing failed for variant={}: {}",
                    saved.getSlug(), ex.getMessage());
        }

        return new Result(saved.getId(), saved.getSlug());
    }
}
