package tj.radolfa.application.readmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Full detail view for a single listing variant.
 * Includes the SKU list (sizes/prices), sibling colour swatches, and product attributes.
 *
 * <p>See {@link ListingVariantDto} for the full price model description.
 * On the detail page each {@link SkuDto} carries its own pricing block so the
 * frontend can swap prices when the user selects a different size.
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
        BigDecimal originalPrice,
        BigDecimal discountPrice,
        Integer discountPercentage,
        String discountName,
        String discountColorHex,
        BigDecimal loyaltyPrice,
        Integer loyaltyPercentage,
        boolean isPartialDiscount,
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

    /**
     * Returns a copy with loyalty pricing stamped on the variant and on every SKU.
     * Called by {@code TierPricingEnricher} for authenticated users who have a tier.
     *
     * <p>Each SKU uses its own {@code discountPercentage} in the best-of formula so
     * the price shown when the user selects a specific size is always correct.
     */
    public ListingVariantDetailDto withLoyalty(BigDecimal loyaltyPct) {
        if (originalPrice == null) return this;

        BigDecimal effectivePct = loyaltyPct.max(
                discountPercentage != null ? BigDecimal.valueOf(discountPercentage) : BigDecimal.ZERO);
        BigDecimal loyalty = originalPrice
                .multiply(BigDecimal.ONE.subtract(
                        effectivePct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);

        List<SkuDto> loyaltySkus = skus.stream()
                .map(sku -> {
                    if (sku.originalPrice() == null) return sku;
                    BigDecimal skuEffectivePct = loyaltyPct.max(
                            sku.discountPercentage() != null
                                    ? BigDecimal.valueOf(sku.discountPercentage())
                                    : BigDecimal.ZERO);
                    BigDecimal skuLoyalty = sku.originalPrice()
                            .multiply(BigDecimal.ONE.subtract(
                                    skuEffectivePct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                            .setScale(2, RoundingMode.HALF_UP);
                    return new SkuDto(sku.skuId(), sku.skuCode(), sku.sizeLabel(),
                            sku.stockQuantity(), sku.originalPrice(), sku.discountPrice(),
                            sku.discountPercentage(), sku.discountName(), sku.discountColorHex(),
                            skuLoyalty);
                })
                .toList();

        return new ListingVariantDetailDto(variantId, slug, colorDisplayName, categoryName,
                colorKey, colorHex, webDescription, images, attributes,
                originalPrice, discountPrice, discountPercentage, discountName, discountColorHex,
                loyalty, loyaltyPct.intValue(), isPartialDiscount,
                topSelling, featured, loyaltySkus, siblingVariants, productCode);
    }
}
