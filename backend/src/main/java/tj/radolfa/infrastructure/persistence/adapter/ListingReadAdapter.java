package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadColorImagesPort;
import tj.radolfa.application.ports.out.LoadListingPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.entity.DiscountEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductVariantEntity;
import tj.radolfa.infrastructure.persistence.repository.ProductVariantRepository;
import tj.radolfa.application.readmodel.ListingVariantDetailDto;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.application.readmodel.SkuDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Hexagonal adapter implementing SQL-backed read queries for listings.
 *
 * <p>Grid queries use native SQL with subquery grouping (one card per template+color).
 * Detail queries load all size-siblings as "SKUs" and color-siblings as swatches.
 * Discounts are resolved post-query from the discounts table.
 */
@Component
public class ListingReadAdapter implements LoadListingPort {

    private final ProductVariantRepository variantRepo;
    private final DiscountEnrichmentAdapter discountEnrichment;
    private final LoadColorImagesPort loadColorImagesPort;

    public ListingReadAdapter(ProductVariantRepository variantRepo,
                              DiscountEnrichmentAdapter discountEnrichment,
                              LoadColorImagesPort loadColorImagesPort) {
        this.variantRepo = variantRepo;
        this.discountEnrichment = discountEnrichment;
        this.loadColorImagesPort = loadColorImagesPort;
    }

    @Override
    public PageResult<ListingVariantDto> loadPage(int page, int limit) {
        Page<Object[]> raw = variantRepo.findGridPage(PageRequest.of(page - 1, limit));
        return toGridResult(raw, page, limit);
    }

    @Override
    public Optional<ListingVariantDetailDto> loadBySlug(String slug) {
        return variantRepo.findBySeoSlug(slug).map(this::toDetailDto);
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

    // ---- Grid helpers ----

    private PageResult<ListingVariantDto> toGridResult(Page<Object[]> raw, int page, int limit) {
        List<Long> variantIds = raw.getContent().stream()
                .map(row -> ListingGridRowMapper.toLong(row[0]))
                .toList();

        Map<Long, DiscountInfo> discountMap = discountEnrichment.resolveForVariants(variantIds);

        List<ListingVariantDto> items = raw.getContent().stream()
                .map(row -> ListingGridRowMapper.toGridDto(row, discountMap))
                .toList();

        return new PageResult<>(items, raw.getTotalElements(), page,
                (long) page * limit < raw.getTotalElements());
    }

    // ---- Detail helpers ----

    private ListingVariantDetailDto toDetailDto(ProductVariantEntity entity) {
        Long templateId = entity.getTemplate().getId();
        String color = entity.getAttributes().getOrDefault("Color", null);

        // Size siblings = "SKUs" (all variants of same template+color)
        List<ProductVariantEntity> sizeSiblings = (color != null)
                ? variantRepo.findSizeSiblings(templateId, color)
                : List.of(entity);

        // Resolve discounts for all item codes
        List<String> itemCodes = sizeSiblings.stream()
                .map(ProductVariantEntity::getErpVariantCode)
                .toList();
        Map<String, DiscountEntity> discountsByItemCode =
                discountEnrichment.resolveForItemCodes(itemCodes);

        List<SkuDto> skus = sizeSiblings.stream()
                .map(v -> toSkuDto(v, discountsByItemCode.get(v.getErpVariantCode())))
                .toList();

        BigDecimal originalPrice = sizeSiblings.stream()
                .map(ProductVariantEntity::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);

        // Find cheapest discounted price across all SKUs
        BigDecimal discountedPrice = null;
        BigDecimal discountPercentage = null;
        String saleTitle = null;
        String saleColorHex = null;

        for (ProductVariantEntity v : sizeSiblings) {
            DiscountEntity discount = discountsByItemCode.get(v.getErpVariantCode());
            if (discount == null || v.getPrice() == null) continue;

            BigDecimal dp = computeDiscountedPrice(v.getPrice(), discount.getDiscountValue());
            if (discountedPrice == null || dp.compareTo(discountedPrice) < 0) {
                discountedPrice = dp;
                discountPercentage = discount.getDiscountValue();
                saleTitle = discount.getTitle();
                saleColorHex = discount.getColorHex();
            }
        }

        int totalStock = sizeSiblings.stream()
                .mapToInt(v -> v.getStockQty() != null ? v.getStockQty() : 0)
                .sum();

        // Color siblings (other colors of same template)
        List<ListingVariantDetailDto.SiblingVariant> siblings;
        if (color != null) {
            List<Object[]> siblingRows = variantRepo.findColorSiblings(templateId, color);
            siblings = siblingRows.stream()
                    .map(row -> {
                        List<String> sibImages = ListingGridRowMapper.parseImages(row[3]);
                        String thumbnail = sibImages.isEmpty() ? null : sibImages.get(0);
                        return new ListingVariantDetailDto.SiblingVariant(
                                (String) row[1],   // slug
                                (String) row[2],   // colorKey
                                null,              // colorHexCode (not in new model)
                                thumbnail);
                    })
                    .toList();
        } else {
            siblings = List.of();
        }

        List<String> images = loadColorImagesPort.findImagesByTemplateAndColor(templateId, color);

        String categoryName = entity.getTemplate().getCategoryName();

        return new ListingVariantDetailDto(
                entity.getId(),
                entity.getSeoSlug(),
                entity.getTemplate().getName(),
                categoryName,
                color,
                null,              // colorHexCode
                entity.getTemplate().getDescription(),
                images,
                originalPrice,
                discountedPrice,
                null,              // loyaltyPrice (enriched by controller)
                discountPercentage,
                null,              // loyaltyDiscountPercentage
                saleTitle,
                saleColorHex,
                totalStock,
                entity.getTemplate().isTopSelling(),
                entity.getTemplate().isFeatured(),
                skus,
                siblings);
    }

    // ---- SKU helpers (detail-page only) ----

    private SkuDto toSkuDto(ProductVariantEntity entity, DiscountEntity discount) {
        boolean onSale = discount != null && entity.getPrice() != null;

        BigDecimal effectiveDiscounted = null;
        BigDecimal effectiveDiscountPct = null;
        if (onSale) {
            effectiveDiscounted = computeDiscountedPrice(
                    entity.getPrice(), discount.getDiscountValue());
            effectiveDiscountPct = discount.getDiscountValue();
        }

        String sizeLabel = entity.getAttributes().getOrDefault("Size", null);

        return new SkuDto(
                entity.getId(),
                entity.getErpVariantCode(),
                sizeLabel,
                entity.getStockQty(),
                entity.getPrice(),
                effectiveDiscounted,
                null,               // loyaltyPrice (enriched by controller)
                effectiveDiscountPct,
                null,               // loyaltyDiscountPercentage
                onSale ? discount.getTitle() : null,
                onSale ? discount.getColorHex() : null,
                onSale,
                onSale ? discount.getValidUpto() : null);
    }

    private BigDecimal computeDiscountedPrice(BigDecimal originalPrice, BigDecimal discountPct) {
        BigDecimal multiplier = BigDecimal.ONE.subtract(
                discountPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return originalPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
