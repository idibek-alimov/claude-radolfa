package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadListingPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.entity.DiscountEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantImageEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;
import tj.radolfa.application.readmodel.ListingVariantDetailDto;
import tj.radolfa.application.readmodel.ListingVariantDetailDto.AttributeDto;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.application.readmodel.SkuDto;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantAttributeEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Hexagonal adapter implementing the SQL-backed read queries for listings.
 *
 * <p>
 * Grid queries use JPQL aggregates (single query, no N+1).
 * Images and SKUs are batch-loaded in separate queries for the page.
 * Discounts are resolved from the discounts table post-query.
 */
@Component
public class ListingReadAdapter implements LoadListingPort {

        private final ListingVariantRepository variantRepo;
        private final SkuRepository skuRepo;
        private final DiscountEnrichmentAdapter discountEnrichment;

        public ListingReadAdapter(ListingVariantRepository variantRepo,
                        SkuRepository skuRepo,
                        DiscountEnrichmentAdapter discountEnrichment) {
                this.variantRepo = variantRepo;
                this.skuRepo = skuRepo;
                this.discountEnrichment = discountEnrichment;
        }

        @Override
        public PageResult<ListingVariantDto> loadPage(int page, int limit) {
                Page<Object[]> raw = variantRepo.findGridPage(PageRequest.of(page - 1, limit));
                return toGridResult(raw, page, limit);
        }

        @Override
        public Optional<ListingVariantDetailDto> loadBySlug(String slug) {
                return variantRepo.findDetailBySlug(slug).map(this::toDetailDto);
        }

        @Override
        public PageResult<ListingVariantDto> search(String query, int page, int limit) {
                Page<Object[]> raw = variantRepo.searchGrid(query, PageRequest.of(page - 1, limit));
                return toGridResult(raw, page, limit);
        }

        @Override
        public List<String> autocomplete(String prefix, int limit) {
                return variantRepo.autocompleteNames(prefix, PageRequest.of(0, limit));
        }

        @Override
        public PageResult<ListingVariantDto> loadByCategoryIds(List<Long> categoryIds, int page, int limit) {
                Page<Object[]> raw = variantRepo.findGridByCategoryIds(categoryIds, PageRequest.of(page - 1, limit));
                return toGridResult(raw, page, limit);
        }

        @Override
        public PageResult<ListingVariantDto> findByProductCode(String code, int page, int limit) {
                Page<Object[]> raw = variantRepo.findGridByProductCode(code, PageRequest.of(page - 1, limit));
                return toGridResult(raw, page, limit);
        }

        // ---- Grid helpers ----

        private PageResult<ListingVariantDto> toGridResult(Page<Object[]> raw, int page, int limit) {
                List<Long> variantIds = raw.getContent().stream()
                                .map(row -> (Long) row[0])
                                .toList();

                Map<Long, List<String>> imageMap = ListingGridRowMapper.loadImageMap(variantIds, variantRepo);
                Map<Long, DiscountInfo> discountMap = discountEnrichment.resolveForVariants(variantIds);
                Map<Long, List<SkuDto>> skuMap = ListingGridRowMapper.loadSkuMap(variantIds, skuRepo);

                List<ListingVariantDto> content = raw.getContent().stream()
                                .map(row -> ListingGridRowMapper.toGridDto(row, imageMap, discountMap, skuMap))
                                .toList();

                return new PageResult<>(content, raw.getTotalElements(), page, limit,
                                (long) page * limit >= raw.getTotalElements());
        }

        // ---- Detail helpers ----

        private ListingVariantDetailDto toDetailDto(ListingVariantEntity entity) {
                List<String> images = entity.getImages().stream()
                                .map(ListingVariantImageEntity::getImageUrl)
                                .toList();

                List<AttributeDto> attributes = entity.getAttributes().stream()
                                .map(a -> new AttributeDto(a.getAttrKey(), a.getAttrValue()))
                                .toList();

                List<SkuEntity> skuEntities = skuRepo.findByListingVariantId(entity.getId());

                // Resolve discounts for all SKU codes in this variant
                List<String> skuCodes = skuEntities.stream()
                                .map(SkuEntity::getSkuCode)
                                .toList();
                Map<String, DiscountEntity> discountsBySkuCode =
                                discountEnrichment.resolveForItemCodes(skuCodes);

                List<SkuDto> skus = skuEntities.stream()
                                .map(sku -> toSkuDto(sku, discountsBySkuCode.get(sku.getSkuCode())))
                                .toList();

                // Find the winning SKU: cheapest effective (discounted) price
                SkuEntity winningSku = null;
                BigDecimal winningEffectivePrice = null;
                for (SkuEntity sku : skuEntities) {
                        if (sku.getOriginalPrice() == null) continue;
                        DiscountEntity d = discountsBySkuCode.get(sku.getSkuCode());
                        BigDecimal effective = d != null
                                        ? computeDiscountedPrice(sku.getOriginalPrice(), d.getDiscountValue())
                                        : sku.getOriginalPrice();
                        if (winningEffectivePrice == null || effective.compareTo(winningEffectivePrice) < 0) {
                                winningEffectivePrice = effective;
                                winningSku = sku;
                        }
                }

                BigDecimal originalPrice = winningSku != null ? winningSku.getOriginalPrice() : null;
                DiscountEntity winningDiscount = winningSku != null
                                ? discountsBySkuCode.get(winningSku.getSkuCode()) : null;
                BigDecimal discountPrice = winningDiscount != null && originalPrice != null
                                ? computeDiscountedPrice(originalPrice, winningDiscount.getDiscountValue()) : null;
                Integer discountPercentage = winningDiscount != null
                                ? winningDiscount.getDiscountValue().intValue() : null;
                String discountName = winningDiscount != null ? winningDiscount.getTitle() : null;
                String discountColorHex = winningDiscount != null ? winningDiscount.getColorHex() : null;

                long discountedCount = skuEntities.stream()
                                .filter(s -> discountsBySkuCode.containsKey(s.getSkuCode()))
                                .count();
                long distinctDiscountIds = skuEntities.stream()
                                .map(s -> discountsBySkuCode.get(s.getSkuCode()))
                                .filter(Objects::nonNull)
                                .map(DiscountEntity::getId)
                                .distinct()
                                .count();
                boolean isPartialDiscount = discountedCount > 0
                                && (discountedCount < skuEntities.size() || distinctDiscountIds > 1);

                // Siblings: load slugs, colorKeys, hexCodes — then batch-load thumbnails
                Long baseId = entity.getProductBase().getId();
                List<Object[]> siblingRows = variantRepo.findSiblings(baseId, entity.getId());

                List<Long> siblingIds = siblingRows.stream()
                                .map(row -> (Long) row[0])
                                .toList();
                Map<Long, List<String>> siblingImageMap = ListingGridRowMapper.loadImageMap(siblingIds, variantRepo);

                List<ListingVariantDetailDto.SiblingVariant> siblings = siblingRows.stream()
                                .map(row -> {
                                        Long sibId = (Long) row[0];
                                        List<String> sibImages = siblingImageMap.getOrDefault(sibId, List.of());
                                        String thumbnail = sibImages.isEmpty() ? null : sibImages.get(0);
                                        return new ListingVariantDetailDto.SiblingVariant(
                                                        (String) row[1],   // slug
                                                        (String) row[2],   // colorKey
                                                        (String) row[3],   // colorHex (hexCode)
                                                        thumbnail);
                                })
                                .toList();

                String categoryName = entity.getProductBase().getCategory() != null
                                ? entity.getProductBase().getCategory().getName()
                                : null;
                String colorKey = entity.getColor() != null
                                ? entity.getColor().getColorKey()
                                : null;
                String colorHex = entity.getColor() != null
                                ? entity.getColor().getHexCode()
                                : null;

                return new ListingVariantDetailDto(
                                entity.getId(),
                                entity.getSlug(),
                                entity.getProductBase().getName(),
                                categoryName,
                                colorKey,
                                colorHex,
                                entity.getWebDescription(),
                                images,
                                attributes,
                                originalPrice,
                                discountPrice,
                                discountPercentage,
                                discountName,
                                discountColorHex,
                                null,              // loyaltyPrice — stamped by TierPricingEnricher
                                null,              // loyaltyPercentage — stamped by TierPricingEnricher
                                isPartialDiscount,
                                entity.isTopSelling(),
                                entity.isFeatured(),
                                skus,
                                siblings,
                                entity.getProductCode());
        }

        // ---- SKU helpers (detail-page only) ----

        private SkuDto toSkuDto(SkuEntity entity, DiscountEntity discount) {
                BigDecimal originalPrice = entity.getOriginalPrice();
                BigDecimal discountPrice = null;
                Integer discountPercentage = null;
                String discountName = null;
                String discountColorHex = null;

                if (discount != null && originalPrice != null) {
                        discountPrice = computeDiscountedPrice(originalPrice, discount.getDiscountValue());
                        discountPercentage = discount.getDiscountValue().intValue();
                        discountName = discount.getTitle();
                        discountColorHex = discount.getColorHex();
                }

                return new SkuDto(
                                entity.getId(),
                                entity.getSkuCode(),
                                entity.getSizeLabel(),
                                entity.getStockQuantity(),
                                originalPrice,
                                discountPrice,
                                discountPercentage,
                                discountName,
                                discountColorHex,
                                null); // loyaltyPrice — stamped by TierPricingEnricher via withLoyalty cascade
        }

        private BigDecimal computeDiscountedPrice(BigDecimal originalPrice, BigDecimal discountPct) {
                BigDecimal multiplier = BigDecimal.ONE.subtract(
                                discountPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                return originalPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        }
}
