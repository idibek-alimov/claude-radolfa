package tj.radolfa.infrastructure.persistence.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveListingVariantPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.ProductAttribute;
import tj.radolfa.infrastructure.persistence.entity.BrandEntity;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;
import tj.radolfa.infrastructure.persistence.entity.ColorEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantAttributeEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantAttributeValueEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantImageEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductBaseEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.mappers.ProductHierarchyMapper;
import tj.radolfa.infrastructure.persistence.entity.ProductTagEntity;
import tj.radolfa.infrastructure.persistence.repository.BrandRepository;
import tj.radolfa.infrastructure.persistence.repository.CategoryRepository;
import tj.radolfa.infrastructure.persistence.repository.ColorRepository;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductBaseRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductTagRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hexagonal adapter bridging the hierarchy out-ports to Spring Data JPA.
 */
@Component
public class ProductHierarchyAdapter
        implements LoadProductBasePort, LoadListingVariantPort, LoadSkuPort,
        SaveProductHierarchyPort, SaveListingVariantPort {

    private static final Logger LOG = LoggerFactory.getLogger(ProductHierarchyAdapter.class);

    private final ProductBaseRepository baseRepo;
    private final ListingVariantRepository variantRepo;
    private final SkuRepository skuRepo;
    private final CategoryRepository categoryRepo;
    private final ColorRepository colorRepo;
    private final BrandRepository brandRepo;
    private final ProductTagRepository tagRepo;
    private final ProductHierarchyMapper mapper;
    private final ProductCodeGenerator codeGenerator;

    public ProductHierarchyAdapter(ProductBaseRepository baseRepo,
            ListingVariantRepository variantRepo,
            SkuRepository skuRepo,
            CategoryRepository categoryRepo,
            ColorRepository colorRepo,
            BrandRepository brandRepo,
            ProductTagRepository tagRepo,
            ProductHierarchyMapper mapper,
            ProductCodeGenerator codeGenerator) {
        this.baseRepo = baseRepo;
        this.variantRepo = variantRepo;
        this.skuRepo = skuRepo;
        this.categoryRepo = categoryRepo;
        this.colorRepo = colorRepo;
        this.brandRepo = brandRepo;
        this.tagRepo = tagRepo;
        this.mapper = mapper;
        this.codeGenerator = codeGenerator;
    }

    // ---- LoadProductBasePort ----

    @Override
    public Optional<ProductBase> findByExternalRef(String externalRef) {
        return baseRepo.findByExternalRef(externalRef)
                .map(mapper::toProductBase);
    }

    @Override
    public Optional<ProductBase> findById(Long id) {
        return baseRepo.findById(id).map(mapper::toProductBase);
    }

    // ---- LoadListingVariantPort ----

    @Override
    public Optional<ListingVariant> findVariantById(Long id) {
        return variantRepo.findById(id).map(mapper::toListingVariant);
    }

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

    @Override
    public List<ListingVariant> findAllByProductBaseId(Long productBaseId) {
        return variantRepo.findByProductBaseId(productBaseId).stream()
                .map(mapper::toListingVariant)
                .toList();
    }

    @Override
    public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) {
        return variantRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        ListingVariantEntity::getId,
                        mapper::toListingVariant));
    }

    // ---- SaveListingVariantPort ----

    @Override
    public void save(ListingVariant variant) {
        ListingVariantEntity entity = variantRepo.findById(variant.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ListingVariant not found: id=" + variant.getId()));

        entity.setWebDescription(variant.getWebDescription());

        // Sync images: replace with domain's current list
        entity.getImages().clear();
        List<String> domainImages = variant.getImages();
        for (int i = 0; i < domainImages.size(); i++) {
            ListingVariantImageEntity img = new ListingVariantImageEntity();
            img.setListingVariant(entity);
            img.setImageUrl(domainImages.get(i));
            img.setSortOrder(i + 1);
            img.setPrimary(i == 0);
            entity.getImages().add(img);
        }

        // Sync attributes: replace with domain's current list
        entity.getAttributes().clear();
        List<ProductAttribute> domainAttrs = variant.getAttributes();
        for (ProductAttribute attr : domainAttrs) {
            ListingVariantAttributeEntity attrEntity = new ListingVariantAttributeEntity();
            attrEntity.setListingVariant(entity);
            attrEntity.setAttrKey(attr.key());
            attrEntity.setSortOrder(attr.sortOrder());
            for (int i = 0; i < attr.values().size(); i++) {
                ListingVariantAttributeValueEntity valueEntity = new ListingVariantAttributeValueEntity();
                valueEntity.setAttribute(attrEntity);
                valueEntity.setValue(attr.values().get(i));
                valueEntity.setSortOrder(i);
                attrEntity.getValues().add(valueEntity);
            }
            entity.getAttributes().add(attrEntity);
        }

        variantRepo.save(entity);
    }

    @Override
    public void saveTags(Long variantId, List<Long> tagIds) {
        ListingVariantEntity entity = variantRepo.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ListingVariant not found: id=" + variantId));

        entity.getTags().clear();
        if (tagIds != null && !tagIds.isEmpty()) {
            Set<ProductTagEntity> tagEntities = new HashSet<>(tagRepo.findAllById(tagIds));
            entity.getTags().addAll(tagEntities);
        }

        variantRepo.save(entity);
    }

    // ---- LoadSkuPort ----

    @Override
    public Optional<Sku> findBySkuCode(String skuCode) {
        return skuRepo.findBySkuCode(skuCode)
                .map(mapper::toSku);
    }

    @Override
    public Optional<Sku> findSkuById(Long id) {
        return skuRepo.findById(id).map(mapper::toSku);
    }

    @Override
    public List<Sku> findSkusByVariantId(Long variantId) {
        return skuRepo.findByListingVariantId(variantId).stream()
                .map(mapper::toSku)
                .toList();
    }

    // ---- SaveProductHierarchyPort ----

    @Override
    public ProductBase saveBase(ProductBase base) {
        ProductBaseEntity entity;

        if (base.getId() != null) {
            // Update existing
            entity = baseRepo.findById(base.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ProductBase not found: id=" + base.getId()));
            entity.setName(base.getName());
        } else {
            // Create new
            entity = mapper.toBaseEntity(base);
        }

        // Resolve category → use ID (native creation / update path) first,
        // fall back to name lookup for the ERP sync path (no ID available).
        if (base.getCategoryId() != null) {
            entity.setCategory(categoryRepo.getReferenceById(base.getCategoryId()));
            entity.setCategoryName(base.getCategory());
        } else if (base.getCategory() != null && !base.getCategory().isBlank()) {
            CategoryEntity categoryEntity = categoryRepo.findByName(base.getCategory())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found: '" + base.getCategory() + "'"));
            entity.setCategory(categoryEntity);
            entity.setCategoryName(categoryEntity.getName());
        } else {
            entity.setCategory(null);
            entity.setCategoryName(null);
        }

        // Resolve brand id -> BrandEntity (Radolfa-managed, never set from ERP sync
        // path)
        // Service already validated brand existence; use getReferenceById to avoid a
        // second DB round-trip.
        if (base.getBrandId() != null) {
            entity.setBrand(brandRepo.getReferenceById(base.getBrandId()));
        } else {
            entity.setBrand(null);
        }

        return mapper.toProductBase(baseRepo.save(entity));
    }

    @Override
    public ListingVariant saveVariant(ListingVariant variant, Long productBaseId) {
        ListingVariantEntity entity;

        if (variant.getId() != null) {
            // Update existing — preserve images and webDescription
            entity = variantRepo.findById(variant.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ListingVariant not found: id=" + variant.getId()));
            entity.setSlug(variant.getSlug());
            entity.setLastSyncAt(variant.getLastSyncAt());
            // Do NOT overwrite webDescription or images — enrichment-owned
        } else {
            // Create new
            entity = mapper.toVariantEntity(variant);
            ProductBaseEntity baseRef = baseRepo.getReferenceById(productBaseId);
            entity.setProductBase(baseRef);
            entity.setProductCode(codeGenerator.generate());

            // Sync attributes on creation
            List<ProductAttribute> domainAttrs = variant.getAttributes();
            if (domainAttrs != null) {
                for (ProductAttribute attr : domainAttrs) {
                    ListingVariantAttributeEntity attrEntity = new ListingVariantAttributeEntity();
                    attrEntity.setListingVariant(entity);
                    attrEntity.setAttrKey(attr.key());
                    attrEntity.setSortOrder(attr.sortOrder());
                    for (int i = 0; i < attr.values().size(); i++) {
                        ListingVariantAttributeValueEntity valueEntity = new ListingVariantAttributeValueEntity();
                        valueEntity.setAttribute(attrEntity);
                        valueEntity.setValue(attr.values().get(i));
                        valueEntity.setSortOrder(i);
                        attrEntity.getValues().add(valueEntity);
                    }
                    entity.getAttributes().add(attrEntity);
                }
            }

            // Sync images on creation
            List<String> domainImages = variant.getImages();
            if (domainImages != null) {
                for (int i = 0; i < domainImages.size(); i++) {
                    ListingVariantImageEntity img = new ListingVariantImageEntity();
                    img.setListingVariant(entity);
                    img.setImageUrl(domainImages.get(i));
                    img.setSortOrder(i + 1);
                    img.setPrimary(i == 0);
                    entity.getImages().add(img);
                }
            }
        }

        // Resolve colorKey String -> ColorEntity.
        // Native product creation always pre-validates color via LoadColorPort, so this
        // fallback is only reached from the ERP sync path where color keys arrive as
        // strings
        // without a prior lookup. Auto-created colors have no hex code — acceptable for
        // sync.
        if (variant.getColorKey() != null) {
            ColorEntity colorEntity = colorRepo.findByColorKey(variant.getColorKey())
                    .orElseGet(() -> {
                        LOG.warn("[PRODUCT-ADAPTER] Auto-creating color '{}' for ERP sync — hex code not set",
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
        if (colorKey == null)
            return null;
        String[] words = colorKey.split("-");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty())
                sb.append(' ');
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1)
                    sb.append(word.substring(1));
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
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sku not found: id=" + sku.getId()));
            entity.setSizeLabel(sku.getSizeLabel());
            entity.setStockQuantity(sku.getStockQuantity());
            entity.setOriginalPrice(sku.getPrice() != null ? sku.getPrice().amount() : null);
        } else {
            // Create new
            entity = mapper.toSkuEntity(sku);
            ListingVariantEntity variantRef = variantRepo.getReferenceById(listingVariantId);
            entity.setListingVariant(variantRef);
        }

        return mapper.toSku(skuRepo.save(entity));
    }
}
