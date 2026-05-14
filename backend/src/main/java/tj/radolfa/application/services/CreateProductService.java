package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.CreateProductUseCase;
import tj.radolfa.domain.exception.InvalidAttributeValueException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.LoadBrandPort;
import tj.radolfa.application.ports.out.LoadCategoryBlueprintPort;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.readmodel.CategoryView;
import tj.radolfa.application.ports.out.LoadColorPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductAttribute;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Creates a full native product hierarchy (ProductBase → ListingVariants → SKUs)
 * without any external system involvement.
 *
 * <p>All variants are persisted within a single transaction. ES indexing is
 * fire-and-forget outside the transaction boundary.</p>
 */
@Slf4j
@Service
public class CreateProductService implements CreateProductUseCase {

    private final LoadCategoryPort          loadCategoryPort;
    private final LoadColorPort             loadColorPort;
    private final LoadBrandPort             loadBrandPort;
    private final LoadCategoryBlueprintPort loadBlueprintPort;
    private final SaveProductHierarchyPort  savePort;
    private final ListingIndexPort          listingIndexPort;

    public CreateProductService(LoadCategoryPort loadCategoryPort,
                                LoadColorPort loadColorPort,
                                LoadBrandPort loadBrandPort,
                                LoadCategoryBlueprintPort loadBlueprintPort,
                                SaveProductHierarchyPort savePort,
                                ListingIndexPort listingIndexPort) {
        this.loadCategoryPort  = loadCategoryPort;
        this.loadColorPort     = loadColorPort;
        this.loadBrandPort     = loadBrandPort;
        this.loadBlueprintPort = loadBlueprintPort;
        this.savePort          = savePort;
        this.listingIndexPort  = listingIndexPort;
    }

    @Override
    @Transactional
    public Long execute(Command command) {
        // 1. Resolve category
        CategoryView category = loadCategoryPort.findById(command.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: id=" + command.categoryId()));

        // 2. Resolve brand (optional — throws if an ID was supplied but not found)
        Long brandId = null;
        if (command.brandId() != null) {
            loadBrandPort.findById(command.brandId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Brand not found: id=" + command.brandId()));
            brandId = command.brandId();
        }

        // 3. Validate attributes against category blueprint (required-key and type checks)
        List<LoadCategoryBlueprintPort.BlueprintEntry> blueprints =
                loadBlueprintPort.findByCategoryId(command.categoryId());

        if (!blueprints.isEmpty()) {
            Map<String, LoadCategoryBlueprintPort.BlueprintEntry> blueprintByKey = blueprints.stream()
                    .collect(Collectors.toMap(LoadCategoryBlueprintPort.BlueprintEntry::attributeKey, e -> e));

            List<String> requiredKeys = blueprints.stream()
                    .filter(LoadCategoryBlueprintPort.BlueprintEntry::required)
                    .map(LoadCategoryBlueprintPort.BlueprintEntry::attributeKey)
                    .toList();

            for (Command.VariantDefinition variantDef : command.variants()) {
                List<ProductAttribute> attrs = variantDef.attributes() == null
                        ? List.of() : variantDef.attributes();
                Set<String> providedKeys = attrs.stream()
                        .map(ProductAttribute::key)
                        .collect(Collectors.toSet());

                List<String> missing = requiredKeys.stream()
                        .filter(k -> !providedKeys.contains(k))
                        .toList();

                if (!missing.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Missing required attributes for category '" + category.name()
                            + "': " + missing);
                }

                validateAttributeValues(attrs, blueprintByKey);
            }
        }

        // 4. Create ProductBase with auto-generated externalRef
        // Use 12 hex chars (48 bits of entropy) to avoid birthday collisions at scale.
        String externalRef = "INTERNAL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        ProductBase base = new ProductBase(null, externalRef, command.name(), category.name(), category.id(), brandId);
        ProductBase savedBase = savePort.saveBase(base);

        log.info("[CREATE-PRODUCT] Created ProductBase id={} name='{}' externalRef={}",
                savedBase.getId(), command.name(), externalRef);

        // 5. Create one ListingVariant + SKUs per variant definition
        for (Command.VariantDefinition variantDef : command.variants()) {
            LoadColorPort.ColorView color = loadColorPort.findById(variantDef.colorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Color not found: id=" + variantDef.colorId()));

            ListingVariant variant = new ListingVariant(
                    null,
                    savedBase.getId(),
                    color.colorKey(),
                    null,                    // slug — generated below
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(), // tagIds — none on creation
                    null,
                    null,                    // productCode — assigned by persistence layer on first save
                    variantDef.isEnabled(),
                    variantDef.isActive(),
                    variantDef.weightKg(),
                    variantDef.widthCm(),
                    variantDef.heightCm(),
                    variantDef.depthCm()
            );

            variant.generateSlug(savedBase.getExternalRef());

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
                String skuCode  = "SKU-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
                String barcode  = "BC-"  + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
                Sku sku = new Sku(
                        null,
                        savedVariant.getId(),
                        skuCode,
                        def.sizeLabel(),
                        def.stockQuantity(),
                        def.price(),
                        barcode
                );
                savedSkus.add(savePort.saveSku(sku, savedVariant.getId()));
            }

            log.info("[CREATE-PRODUCT] Created variant slug='{}' with {} SKU(s)",
                    savedVariant.getSlug(), savedSkus.size());

            // ES indexing — fire-and-forget, outside transaction boundary by design
            try {
                indexVariant(savedVariant, command.name(), category.name(), color.hexCode(), savedSkus);
            } catch (Exception ex) {
                log.error("[CREATE-PRODUCT] ES indexing failed for variant={}: {}",
                        savedVariant.getSlug(), ex.getMessage());
            }
        }

        return savedBase.getId();
    }

    private void validateAttributeValues(
            List<ProductAttribute> attrs,
            Map<String, LoadCategoryBlueprintPort.BlueprintEntry> blueprintByKey) {

        for (ProductAttribute attr : attrs) {
            LoadCategoryBlueprintPort.BlueprintEntry bp = blueprintByKey.get(attr.key());
            if (bp == null) continue; // attribute not in blueprint — permitted

            switch (bp.type()) {
                case ENUM -> {
                    if (attr.values().size() != 1) {
                        throw new InvalidAttributeValueException(attr.key(),
                                "ENUM attributes must have exactly one value, got: " + attr.values());
                    }
                    String value = attr.values().get(0);
                    if (!bp.allowedValues().contains(value)) {
                        throw new InvalidAttributeValueException(attr.key(),
                                "'" + value + "' is not an allowed value. Allowed: " + bp.allowedValues());
                    }
                }
                case MULTI -> {
                    for (String value : attr.values()) {
                        if (!bp.allowedValues().contains(value)) {
                            throw new InvalidAttributeValueException(attr.key(),
                                    "'" + value + "' is not an allowed value. Allowed: " + bp.allowedValues());
                        }
                    }
                }
                case NUMBER -> {
                    if (attr.values().size() != 1) {
                        throw new InvalidAttributeValueException(attr.key(),
                                "NUMBER attributes must have exactly one value, got: " + attr.values());
                    }
                    try {
                        new java.math.BigDecimal(attr.values().get(0));
                    } catch (NumberFormatException e) {
                        throw new InvalidAttributeValueException(attr.key(),
                                "'" + attr.values().get(0) + "' is not a valid number");
                    }
                }
                case TEXT -> { /* no constraint beyond presence */ }
            }
        }
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

        List<String> skuCodes = skus.stream()
                .map(Sku::getSkuCode)
                .filter(java.util.Objects::nonNull)
                .toList();

        listingIndexPort.index(
                variant.getId(),
                variant.getProductBaseId(),
                variant.getSlug(),
                productName,
                category,
                variant.getColorKey(),
                colorHex,
                variant.getWebDescription(),
                new java.util.ArrayList<>(variant.getImages()),
                price,
                totalStock,
                variant.getLastSyncAt(),
                variant.getProductCode(),
                skuCodes
        );
    }
}
