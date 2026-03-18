package tj.radolfa.application.readmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Full detail view for a single listing variant.
 * Includes the SKU list (sizes/prices), sibling colour swatches, and product attributes.
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
        List<AttributeDto> attributes,
        BigDecimal originalPrice,
        BigDecimal discountedPrice,
        BigDecimal loyaltyPrice,
        BigDecimal discountPercentage,
        BigDecimal loyaltyDiscountPercentage,
        String saleTitle,
        String saleColorHex,
        Integer totalStock,
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
            String colorHexCode,
            String thumbnail
    ) {}

    public ListingVariantDetailDto withLoyaltyPrice(BigDecimal loyaltyPct) {
        if (loyaltyPct.compareTo(BigDecimal.ZERO) == 0) return this;

        BigDecimal base = discountedPrice != null ? discountedPrice : originalPrice;
        if (base == null) return this;

        BigDecimal multiplier = BigDecimal.ONE.subtract(
                loyaltyPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal lp = base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        List<SkuDto> enrichedSkus = skus.stream()
                .map(sku -> sku.withLoyaltyPrice(loyaltyPct))
                .toList();

        return new ListingVariantDetailDto(id, slug, name, category, colorKey, colorHexCode,
                webDescription, images, attributes, originalPrice, discountedPrice, lp,
                discountPercentage, loyaltyPct, saleTitle, saleColorHex,
                totalStock, topSelling, featured, enrichedSkus, siblingVariants, productCode);
    }
}
