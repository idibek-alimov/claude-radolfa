package tj.radolfa.infrastructure.persistence.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadProductTemplatePort;
import tj.radolfa.application.ports.out.LoadProductVariantPort;
import tj.radolfa.application.ports.out.SaveProductPort;
import tj.radolfa.domain.model.ProductTemplate;
import tj.radolfa.domain.model.ProductVariant;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductTemplateEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductVariantEntity;
import tj.radolfa.infrastructure.persistence.mappers.ProductMapper;
import tj.radolfa.infrastructure.persistence.repository.CategoryRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductTemplateRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductVariantRepository;

import java.util.Optional;

/**
 * Hexagonal adapter bridging the product out-ports to Spring Data JPA.
 */
@Component
public class ProductPersistenceAdapter
        implements LoadProductTemplatePort, LoadProductVariantPort, SaveProductPort {

    private static final Logger LOG = LoggerFactory.getLogger(ProductPersistenceAdapter.class);

    private final ProductTemplateRepository templateRepo;
    private final ProductVariantRepository  variantRepo;
    private final CategoryRepository        categoryRepo;
    private final ProductMapper             mapper;

    public ProductPersistenceAdapter(ProductTemplateRepository templateRepo,
                                     ProductVariantRepository variantRepo,
                                     CategoryRepository categoryRepo,
                                     ProductMapper mapper) {
        this.templateRepo = templateRepo;
        this.variantRepo  = variantRepo;
        this.categoryRepo = categoryRepo;
        this.mapper       = mapper;
    }

    // ---- LoadProductTemplatePort ----

    @Override
    public Optional<ProductTemplate> findByErpTemplateCode(String erpTemplateCode) {
        return templateRepo.findByErpTemplateCode(erpTemplateCode)
                .map(mapper::toProductTemplate);
    }

    @Override
    public Optional<ProductTemplate> findById(Long id) {
        return templateRepo.findById(id)
                .map(mapper::toProductTemplate);
    }

    // ---- LoadProductVariantPort ----

    @Override
    public Optional<ProductVariant> findByErpVariantCode(String erpVariantCode) {
        return variantRepo.findByErpVariantCode(erpVariantCode)
                .map(mapper::toProductVariant);
    }

    @Override
    public Optional<ProductVariant> findBySlug(String slug) {
        return variantRepo.findBySeoSlug(slug)
                .map(mapper::toProductVariant);
    }

    // ---- SaveProductPort ----

    @Override
    public ProductTemplate saveTemplate(ProductTemplate template) {
        ProductTemplateEntity entity;

        if (template.getId() != null) {
            entity = templateRepo.findById(template.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "ProductTemplate not found: " + template.getId()));
            entity.setName(template.getName());
            entity.setDescription(template.getDescription());
            entity.setActive(template.isActive());
            entity.setTopSelling(template.isTopSelling());
            entity.setFeatured(template.isFeatured());
            entity.setAttributesDefinition(template.getAttributesDefinition());
        } else {
            entity = mapper.toTemplateEntity(template);
        }

        // Resolve category String -> CategoryEntity
        if (template.getCategory() != null && !template.getCategory().isBlank()) {
            CategoryEntity categoryEntity = categoryRepo.findByName(template.getCategory())
                    .orElseGet(() -> {
                        LOG.warn("[PRODUCT-SYNC] Category '{}' not found — skipping FK",
                                template.getCategory());
                        return null;
                    });
            entity.setCategory(categoryEntity);
            entity.setCategoryName(categoryEntity != null ? categoryEntity.getName() : template.getCategory());
        } else {
            entity.setCategory(null);
            entity.setCategoryName(null);
        }

        return mapper.toProductTemplate(templateRepo.save(entity));
    }

    @Override
    public ProductVariant saveVariant(ProductVariant variant, Long templateId) {
        ProductVariantEntity entity;

        if (variant.getId() != null) {
            entity = variantRepo.findById(variant.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "ProductVariant not found: " + variant.getId()));
            entity.setAttributes(variant.getAttributes());
            entity.setPrice(variant.getPrice() != null ? variant.getPrice().amount() : null);
            entity.setStockQty(variant.getStockQty());
            entity.setActive(variant.isActive());
            entity.setSeoSlug(variant.getSeoSlug());
            entity.setLastSyncAt(variant.getLastSyncAt());
        } else {
            entity = mapper.toVariantEntity(variant);
            ProductTemplateEntity templateRef = templateRepo.getReferenceById(templateId);
            entity.setTemplate(templateRef);
        }

        return mapper.toProductVariant(variantRepo.save(entity));
    }
}
