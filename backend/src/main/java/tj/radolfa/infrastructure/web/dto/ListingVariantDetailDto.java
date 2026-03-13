package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Full detail view for a single listing variant.
 * Includes the SKU list (sizes/prices) and sibling colour swatches.
 *
 * <p>Returned by {@code GET /api/v1/listings/{slug}}.
 */
public record ListingVariantDetailDto(
        Long id,
        String slug,
        String name,
        String category,
        String colorKey,
        String colorHexCode,
        String webDescription,
        List<String> images,
        BigDecimal priceStart,
        BigDecimal priceEnd,
        BigDecimal tierPriceStart,
        BigDecimal tierPriceEnd,
        Integer totalStock,
        boolean topSelling,
        boolean featured,
        List<SkuDto> skus,
        List<SiblingVariant> siblingVariants
) {
    /**
     * Lightweight reference to another colour variant of the same product.
     * Enables the frontend to render colour swatches without a second API call.
     */
    public record SiblingVariant(
            String slug,
            String colorKey,
            String colorHexCode,
            String thumbnail
    ) {}

    public ListingVariantDetailDto withTierDiscount(BigDecimal discountPercentage) {
        if (discountPercentage.compareTo(BigDecimal.ZERO) == 0) return this;

        BigDecimal multiplier = BigDecimal.ONE.subtract(
                discountPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        BigDecimal tpStart = priceStart != null
                ? priceStart.multiply(multiplier).setScale(2, RoundingMode.HALF_UP) : null;
        BigDecimal tpEnd = priceEnd != null
                ? priceEnd.multiply(multiplier).setScale(2, RoundingMode.HALF_UP) : null;

        List<SkuDto> enrichedSkus = skus.stream()
                .map(sku -> sku.withTierDiscount(discountPercentage))
                .toList();

        return new ListingVariantDetailDto(id, slug, name, category, colorKey, colorHexCode,
                webDescription, images, priceStart, priceEnd, tpStart, tpEnd,
                totalStock, topSelling, featured, enrichedSkus, siblingVariants);
    }
}
