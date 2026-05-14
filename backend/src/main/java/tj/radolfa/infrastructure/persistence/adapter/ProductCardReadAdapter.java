package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadProductCardPort;
import tj.radolfa.application.readmodel.ProductCardDto;
import tj.radolfa.application.readmodel.ProductCardDto.AttributeDto;
import tj.radolfa.application.readmodel.ProductCardDto.ImageRef;
import tj.radolfa.application.readmodel.ProductCardDto.SkuSummary;
import tj.radolfa.application.readmodel.ProductCardDto.TagView;
import tj.radolfa.application.readmodel.ProductCardDto.VariantSummary;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantAttributeEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantAttributeValueEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantImageEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductBaseEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductTagEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductBaseRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * SQL-backed adapter for {@link LoadProductCardPort}.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Fetch ProductBase (with category + brand eagerly via JOIN FETCH) — 1 query.</li>
 *   <li>Fetch all ListingVariants for the base (with color + productBase eagerly) — 1 query;
 *       images, attributes, tags, skus are batch-loaded by Hibernate via {@code @BatchSize(50)}.</li>
 *   <li>Assemble the DTO in memory.</li>
 * </ol>
 * Expected cardinality is small (≤ 10 variants × ≤ 20 SKUs), so this is appropriate.
 */
@Component
public class ProductCardReadAdapter implements LoadProductCardPort {

    private final ProductBaseRepository productBaseRepo;
    private final ListingVariantRepository variantRepo;

    public ProductCardReadAdapter(ProductBaseRepository productBaseRepo,
                                  ListingVariantRepository variantRepo) {
        this.productBaseRepo = productBaseRepo;
        this.variantRepo = variantRepo;
    }

    @Override
    public Optional<ProductCardDto> loadByProductBaseId(Long productBaseId) {
        Optional<ProductBaseEntity> baseOpt = productBaseRepo.findById(productBaseId);
        if (baseOpt.isEmpty()) {
            return Optional.empty();
        }
        ProductBaseEntity base = baseOpt.get();

        List<ListingVariantEntity> variants =
                variantRepo.findAllByProductBaseIdForAdmin(productBaseId);

        String brandName = base.getBrand() != null ? base.getBrand().getName() : null;
        Long categoryId  = base.getCategory() != null ? base.getCategory().getId() : null;
        String catName   = base.getCategory() != null ? base.getCategory().getName() : null;

        List<VariantSummary> variantSummaries = variants.stream()
                .map(this::toVariantSummary)
                .toList();

        return Optional.of(new ProductCardDto(
                base.getId(),
                base.getName(),
                brandName,
                categoryId,
                catName,
                variantSummaries));
    }

    private VariantSummary toVariantSummary(ListingVariantEntity v) {
        List<ImageRef> images = v.getImages().stream()
                .sorted(java.util.Comparator.comparingInt(ListingVariantImageEntity::getSortOrder))
                .map(img -> new ImageRef(img.getId(), img.getImageUrl()))
                .toList();

        List<AttributeDto> attributes = v.getAttributes().stream()
                .map(a -> new AttributeDto(
                        a.getAttrKey(),
                        a.getValues().stream()
                                .map(ListingVariantAttributeValueEntity::getValue)
                                .toList()))
                .toList();

        List<TagView> tags = v.getTags().stream()
                .map(t -> new TagView(t.getId(), t.getName(), t.getColorHex()))
                .toList();

        List<SkuSummary> skus = v.getSkus().stream()
                .map(s -> new SkuSummary(
                        s.getId(),
                        s.getSkuCode(),
                        s.getSizeLabel(),
                        s.getStockQuantity(),
                        s.getOriginalPrice()))
                .toList();

        return new VariantSummary(
                v.getId(),
                v.getSlug(),
                v.getProductCode(),
                v.getColor().getId(),
                v.getColor().getColorKey(),
                v.getColor().getDisplayName(),
                v.getColor().getHexCode(),
                v.getWebDescription(),
                images,
                attributes,
                tags,
                skus,
                v.isEnabled(),
                v.isActive(),
                v.getWeightKg(),
                v.getWidthCm(),
                v.getHeightCm(),
                v.getDepthCm());
    }
}
