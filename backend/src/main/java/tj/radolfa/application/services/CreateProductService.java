package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.CreateProductUseCase;
import tj.radolfa.application.ports.out.ListingIndexPort;
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
 * Creates a full native product hierarchy (ProductBase → ListingVariant → SKUs)
 * without any external system involvement.
 */
@Service
public class CreateProductService implements CreateProductUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CreateProductService.class);

    private final LoadCategoryPort         loadCategoryPort;
    private final LoadColorPort            loadColorPort;
    private final SaveProductHierarchyPort savePort;
    private final ListingIndexPort         listingIndexPort;

    public CreateProductService(LoadCategoryPort loadCategoryPort,
                                LoadColorPort loadColorPort,
                                SaveProductHierarchyPort savePort,
                                ListingIndexPort listingIndexPort) {
        this.loadCategoryPort  = loadCategoryPort;
        this.loadColorPort     = loadColorPort;
        this.savePort          = savePort;
        this.listingIndexPort  = listingIndexPort;
    }

    @Override
    @Transactional
    public CreateProductUseCase.Result execute(Command command) {
        // 1. Resolve category
        LoadCategoryPort.CategoryView category = loadCategoryPort.findById(command.categoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found: id=" + command.categoryId()));

        // 2. Resolve color
        LoadColorPort.ColorView color = loadColorPort.findById(command.colorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Color not found: id=" + command.colorId()));

        // 3. Create ProductBase with auto-generated externalRef
        String externalRef = "INTERNAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ProductBase base = new ProductBase(null, externalRef, command.name(), category.name());
        ProductBase savedBase = savePort.saveBase(base);

        LOG.info("[CREATE-PRODUCT] Created ProductBase id={} name='{}' externalRef={}",
                savedBase.getId(), command.name(), externalRef);

        // 4. Create ListingVariant
        ListingVariant variant = new ListingVariant(
                null,
                savedBase.getId(),
                color.colorKey(),
                null,                           // slug — generated below
                command.webDescription(),       // optional web description from create request
                Collections.emptyList(),
                Collections.emptyList(),
                false,
                false,
                null,
                null                            // productCode — assigned by persistence layer on first save
        );
        variant.generateSlug(command.name());   // slug = slugify(name + "-" + colorKey)
        ListingVariant savedVariant = savePort.saveVariant(variant, savedBase.getId());

        // 5. Create SKUs
        List<Sku> savedSkus = new ArrayList<>();
        for (Command.SkuDefinition def : command.skus()) {
            Sku sku = new Sku(
                    null,
                    savedVariant.getId(),
                    "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    def.sizeLabel(),
                    def.stockQuantity(),
                    def.price()
            );
            savedSkus.add(savePort.saveSku(sku, savedVariant.getId()));
        }

        // 6. Index into Elasticsearch (fire-and-forget)
        try {
            indexVariant(savedVariant, command.name(), category.name(), color.hexCode(), savedSkus);
        } catch (Exception ex) {
            LOG.error("[CREATE-PRODUCT] ES indexing failed for variant={}: {}",
                    savedVariant.getSlug(), ex.getMessage());
        }

        return new CreateProductUseCase.Result(savedBase.getId(), savedVariant.getId(), savedVariant.getSlug());
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
