package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;
import tj.radolfa.infrastructure.persistence.entity.ColorEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductBaseEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.mappers.ProductHierarchyMapper;
import tj.radolfa.infrastructure.persistence.repository.CategoryRepository;
import tj.radolfa.infrastructure.persistence.repository.ColorRepository;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductBaseRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;

import java.util.Optional;

/**
 * Hexagonal adapter bridging the hierarchy out-ports to Spring Data JPA.
 */
@Component
public class ProductHierarchyAdapter
        implements LoadProductBasePort, LoadListingVariantPort, LoadSkuPort, SaveProductHierarchyPort {

    private final ProductBaseRepository    baseRepo;
    private final ListingVariantRepository variantRepo;
    private final SkuRepository            skuRepo;
    private final CategoryRepository       categoryRepo;
    private final ColorRepository          colorRepo;
    private final ProductHierarchyMapper   mapper;

    public ProductHierarchyAdapter(ProductBaseRepository baseRepo,
                                   ListingVariantRepository variantRepo,
                                   SkuRepository skuRepo,
                                   CategoryRepository categoryRepo,
                                   ColorRepository colorRepo,
                                   ProductHierarchyMapper mapper) {
        this.baseRepo     = baseRepo;
        this.variantRepo  = variantRepo;
        this.skuRepo      = skuRepo;
        this.categoryRepo = categoryRepo;
        this.colorRepo    = colorRepo;
        this.mapper       = mapper;
    }

    // ---- LoadProductBasePort ----

    @Override
    public Optional<ProductBase> findByErpTemplateCode(String erpTemplateCode) {
        return baseRepo.findByErpTemplateCode(erpTemplateCode)
                .map(mapper::toProductBase);
    }

    // ---- LoadListingVariantPort ----

    @Override
    public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long productBaseId, String colorKey) {
        return variantRepo.findByProductBaseIdAndColorKey(productBaseId, colorKey)
                .map(mapper::toListingVariant);
    }

    // ---- LoadSkuPort ----

    @Override
    public Optional<Sku> findByErpItemCode(String erpItemCode) {
        return skuRepo.findByErpItemCode(erpItemCode)
                .map(mapper::toSku);
    }

    // ---- SaveProductHierarchyPort ----

    @Override
    public ProductBase saveBase(ProductBase base) {
        ProductBaseEntity entity;

        if (base.getId() != null) {
            // Update existing
            entity = baseRepo.findById(base.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "ProductBase not found: " + base.getId()));
            entity.setName(base.getName());
        } else {
            // Create new
            entity = mapper.toBaseEntity(base);
        }

        // Resolve category String -> CategoryEntity (find-or-create placeholder)
        if (base.getCategory() != null && !base.getCategory().isBlank()) {
            CategoryEntity categoryEntity = categoryRepo.findByName(base.getCategory())
                    .orElseGet(() -> {
                        CategoryEntity placeholder = new CategoryEntity();
                        placeholder.setName(base.getCategory());
                        placeholder.setSlug(slugify(base.getCategory()));
                        // parent = null → top-level placeholder
                        return categoryRepo.save(placeholder);
                    });
            entity.setCategory(categoryEntity);
        } else {
            entity.setCategory(null);
        }

        return mapper.toProductBase(baseRepo.save(entity));
    }

    @Override
    public ListingVariant saveVariant(ListingVariant variant, Long productBaseId) {
        ListingVariantEntity entity;

        if (variant.getId() != null) {
            // Update existing — preserve images and webDescription
            entity = variantRepo.findById(variant.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "ListingVariant not found: " + variant.getId()));
            entity.setSlug(variant.getSlug());
            entity.setLastSyncAt(variant.getLastSyncAt());
            // Do NOT overwrite webDescription or images — enrichment-owned
        } else {
            // Create new
            entity = mapper.toVariantEntity(variant);
            ProductBaseEntity baseRef = baseRepo.getReferenceById(productBaseId);
            entity.setProductBase(baseRef);
        }

        // Resolve colorKey String -> ColorEntity (auto-create if missing)
        if (variant.getColorKey() != null) {
            ColorEntity colorEntity = colorRepo.findByColorKey(variant.getColorKey())
                    .orElseGet(() -> {
                        ColorEntity newColor = new ColorEntity();
                        newColor.setColorKey(variant.getColorKey());
                        newColor.setDisplayName(humanize(variant.getColorKey()));
                        return colorRepo.save(newColor);
                    });
            entity.setColor(colorEntity);
        }

        return mapper.toListingVariant(variantRepo.save(entity));
    }

    private String slugify(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private String humanize(String colorKey) {
        if (colorKey == null) return null;
        String[] words = colorKey.split("-");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(' ');
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }

    @Override
    public Sku saveSku(Sku sku, Long listingVariantId) {
        SkuEntity entity;

        if (sku.getId() != null) {
            // Update existing — always overwrite price/stock
            entity = skuRepo.findById(sku.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Sku not found: " + sku.getId()));
            entity.setSizeLabel(sku.getSizeLabel());
            entity.setStockQuantity(sku.getStockQuantity());
            entity.setPrice(sku.getPrice() != null ? sku.getPrice().amount() : null);
            entity.setSalePrice(sku.getSalePrice() != null ? sku.getSalePrice().amount() : null);
            entity.setSaleEndsAt(sku.getSaleEndsAt());
        } else {
            // Create new
            entity = mapper.toSkuEntity(sku);
            ListingVariantEntity variantRef = variantRepo.getReferenceById(listingVariantId);
            entity.setListingVariant(variantRef);
        }

        return mapper.toSku(skuRepo.save(entity));
    }
}
