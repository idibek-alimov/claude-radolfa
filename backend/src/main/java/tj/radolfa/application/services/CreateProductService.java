package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.CreateProductUseCase;
import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.LoadBrandPort;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.ports.out.LoadColorPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Creates a full native product hierarchy (ProductBase → ListingVariants → SKUs)
 * without any external system involvement.
 *
 * <p>All variants are persisted within a single transaction. ES indexing is
 * fire-and-forget outside the transaction boundary.</p>
 */
@Service
public class CreateProductService implements CreateProductUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CreateProductService.class);

    private final LoadCategoryPort         loadCategoryPort;
    private final LoadColorPort            loadColorPort;
    private final LoadBrandPort            loadBrandPort;
    private final SaveProductHierarchyPort savePort;
    private final ListingIndexPort         listingIndexPort;

    public CreateProductService(LoadCategoryPort loadCategoryPort,
                                LoadColorPort loadColorPort,
                                LoadBrandPort loadBrandPort,
                                SaveProductHierarchyPort savePort,
                                ListingIndexPort listingIndexPort) {
        this.loadCategoryPort = loadCategoryPort;
        this.loadColorPort    = loadColorPort;
        this.loadBrandPort    = loadBrandPort;
        this.savePort         = savePort;
        this.listingIndexPort = listingIndexPort;
    }

    @Override
    @Transactional
    public Long execute(Command command) {
        // 1. Resolve category
        LoadCategoryPort.CategoryView category = loadCategoryPort.findById(command.categoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found: id=" + command.categoryId()));

        // 2. Resolve brand (optional — throws if an ID was supplied but not found)
        Long brandId = null;
        if (command.brandId() != null) {
            loadBrandPort.findById(command.brandId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Brand not found: id=" + command.brandId()));
            brandId = command.brandId();
        }

        // 3. Create ProductBase with auto-generated externalRef
        String externalRef = "INTERNAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ProductBase base = new ProductBase(null, externalRef, command.name(), category.name(), brandId);
        ProductBase savedBase = savePort.saveBase(base);

        LOG.info("[CREATE-PRODUCT] Created ProductBase id={} name='{}' externalRef={}",
                savedBase.getId(), command.name(), externalRef);

        // 4. Create one ListingVariant + SKUs per variant definition
        for (Command.VariantDefinition variantDef : command.variants()) {
            LoadColorPort.ColorView color = loadColorPort.findById(variantDef.colorId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Color not found: id=" + variantDef.colorId()));

            ListingVariant variant = new ListingVariant(
                    null,
                    savedBase.getId(),
                    color.colorKey(),
                    null,                    // slug — generated below
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    false,
                    false,
                    null,
                    null                     // productCode — assigned by persistence layer on first save
            );

            variant.generateSlug(command.name());

            if (variantDef.webDescription() != null) {
                variant.updateWebDescription(variantDef.webDescription());
            }

            if (variantDef.attributes() != null && !variantDef.attributes().isEmpty()) {
                variant.setAttributes(variantDef.attributes());
            }

            if (variantDef.images() != null) {
                for (String url : variantDef.images()) {
                    variant.addImage(url);
                }
            }

            ListingVariant savedVariant = savePort.saveVariant(variant, savedBase.getId());

            List<Sku> savedSkus = new ArrayList<>();
            for (Command.SkuDefinition def : variantDef.skus()) {
                Sku sku = new Sku(
                        null,
                        savedVariant.getId(),
                        "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                        def.sizeLabel(),
                        def.stockQuantity(),
                        def.price(),
                        def.barcode(),
                        def.weightKg(),
                        def.widthCm(),
                        def.heightCm(),
                        def.depthCm()
                );
                savedSkus.add(savePort.saveSku(sku, savedVariant.getId()));
            }

            LOG.info("[CREATE-PRODUCT] Created variant slug='{}' with {} SKU(s)",
                    savedVariant.getSlug(), savedSkus.size());

            // ES indexing — fire-and-forget, outside transaction boundary by design
            try {
                indexVariant(savedVariant, command.name(), category.name(), color.hexCode(), savedSkus);
            } catch (Exception ex) {
                LOG.error("[CREATE-PRODUCT] ES indexing failed for variant={}: {}",
                        savedVariant.getSlug(), ex.getMessage());
            }
        }

        return savedBase.getId();
    }

    private void indexVariant(ListingVariant variant, String productName, String category,
                               String colorHex, List<Sku> skus) {
        Double price = skus.stream()
                .map(Sku::getPrice)
                .filter(java.util.Objects::nonNull)
                .map(Money::amount)
                .min(java.math.BigDecimal::compareTo)
                .map(java.math.BigDecimal::doubleValue)
                .orElse(null);

        int totalStock = skus.stream()
                .mapToInt(s -> s.getStockQuantity() != null ? s.getStockQuantity() : 0)
                .sum();

        listingIndexPort.index(
                variant.getId(),
                variant.getSlug(),
                productName,
                category,
                variant.getColorKey(),
                colorHex,
                null,
                List.of(),
                price,
                totalStock,
                false,
                false,
                null
        );
    }
}
