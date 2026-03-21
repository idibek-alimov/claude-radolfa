package tj.radolfa.application.readmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Full detail view for a single listing variant.
 * Includes the SKU list (sizes/prices), sibling colour swatches, and product attributes.
 */
public record ListingVariantDetailDto(
        Long variantId,
        String slug,
        String colorDisplayName,
        String categoryName,
        String colorKey,
        String colorHex,
        String webDescription,
        List<String> images,
        List<AttributeDto> attributes,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal tierDiscountedMinPrice,
        boolean topSelling,
        boolean featured,
        List<SkuDto> skus,
        List<SiblingVariant> siblingVariants,
        String productCode
) {
    /**
     * A single product attribute shown on the detail page.
     * Examples: key="Material" value="Organic Wool", key="Fit" value="Oversized".
     */
    public record AttributeDto(String key, String value) {}

    /**
     * Lightweight reference to another colour variant of the same product.
     * Enables the frontend to render colour swatches without a second API call.
     */
    public record SiblingVariant(
            String slug,
            String colorKey,
            String colorHex,
            String thumbnail
    ) {}

    public ListingVariantDetailDto withLoyaltyPrice(BigDecimal loyaltyPct) {
        if (loyaltyPct.compareTo(BigDecimal.ZERO) == 0) return this;

        BigDecimal multiplier = BigDecimal.ONE.subtract(
                loyaltyPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal tierPrice = minPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        return new ListingVariantDetailDto(variantId, slug, colorDisplayName, categoryName,
                colorKey, colorHex, webDescription, images, attributes,
                minPrice, maxPrice, tierPrice,
                topSelling, featured, skus, siblingVariants, productCode);
    }
}
