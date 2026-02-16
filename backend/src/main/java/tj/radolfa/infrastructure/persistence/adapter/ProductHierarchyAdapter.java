package tj.radolfa.infrastructure.persistence.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveListingVariantPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;
import tj.radolfa.infrastructure.persistence.entity.ColorEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantImageEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductBaseEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.mappers.ProductHierarchyMapper;
import tj.radolfa.infrastructure.persistence.repository.CategoryRepository;
import tj.radolfa.infrastructure.persistence.repository.ColorRepository;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductBaseRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;

import java.util.List;
import java.util.Optional;

/**
 * Hexagonal adapter bridging the hierarchy out-ports to Spring Data JPA.
 */
@Component
public class ProductHierarchyAdapter
        implements LoadProductBasePort, LoadListingVariantPort, LoadSkuPort,
                   SaveProductHierarchyPort, SaveListingVariantPort {

    private static final Logger LOG = LoggerFactory.getLogger(ProductHierarchyAdapter.class);

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

    @Override
    public Optional<ListingVariant> findBySlug(String slug) {
        return variantRepo.findBySlug(slug)
                .map(mapper::toListingVariant);
    }

    // ---- SaveListingVariantPort ----

    @Override
    public void save(ListingVariant variant) {
        ListingVariantEntity entity = variantRepo.findById(variant.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "ListingVariant not found: " + variant.getId()));

        entity.setWebDescription(variant.getWebDescription());
        entity.setTopSelling(variant.isTopSelling());
        entity.setFeatured(variant.isFeatured());

        // Sync images: replace with domain's current list
        entity.getImages().clear();
        List<String> domainImages = variant.getImages();
        for (int i = 0; i < domainImages.size(); i++) {
            ListingVariantImageEntity img = new ListingVariantImageEntity();
            img.setListingVariant(entity);
            img.setImageUrl(domainImages.get(i));
            img.setSortOrder(i + 1);
            entity.getImages().add(img);
        }

        variantRepo.save(entity);
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
                    .orElseThrow(() -> new IllegalStateException(
                            "Category not found: '" + base.getCategory() + "'. Sync categories first."));
            entity.setCategory(categoryEntity);
            entity.setCategoryName(categoryEntity.getName());
        } else {
            entity.setCategory(null);
            entity.setCategoryName(null);
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
                        LOG.warn("[PRODUCT-SYNC] Auto-creating color '{}' — hex code not set",
                                variant.getColorKey());
                        ColorEntity newColor = new ColorEntity();
                        newColor.setColorKey(variant.getColorKey());
                        newColor.setDisplayName(humanize(variant.getColorKey()));
                        return colorRepo.save(newColor);
                    });
            entity.setColor(colorEntity);
        }

        return mapper.toListingVariant(variantRepo.save(entity));
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
