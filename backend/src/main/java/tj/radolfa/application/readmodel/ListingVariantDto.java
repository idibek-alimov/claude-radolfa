package tj.radolfa.application.readmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Product card shown on the grid/listing page.
 * One card per colour variant (e.g. "T-Shirt — Red").
 *
 * <p>Price model:
 * <ul>
 *   <li>{@code originalPrice} — pre-discount price of the cheapest-when-discounted SKU.
 *       Always populated. Show as strikethrough when {@code discountPrice} or
 *       {@code loyaltyPrice} is present.
 *   <li>{@code discountPrice} — sale price. {@code null} when no active discount.
 *   <li>{@code discountPercentage}, {@code discountName}, {@code discountColorHex}
 *       — sale campaign info. {@code null} when no active discount.
 *   <li>{@code loyaltyPrice} — "Your Price" for authenticated users with a loyalty tier.
 *       Formula: {@code originalPrice × (1 − max(discountPct, loyaltyPct) / 100)}.
 *       {@code null} for guests and users without a tier.
 *   <li>{@code loyaltyPercentage} — the user's own tier discount %, not the effective %.
 *       {@code null} for guests and users without a tier.
 *   <li>{@code isPartialDiscount} — {@code true} when not all SKUs of this variant
 *       share the same active discount campaign (some sizes may be at full price).
 * </ul>
 */
public record ListingVariantDto(
        Long productBaseId,
        Long variantId,
        String slug,
        String colorDisplayName,
        String categoryName,
        String colorKey,
        String colorHex,
        String webDescription,
        List<String> images,
        BigDecimal originalPrice,
        BigDecimal discountPrice,
        Integer discountPercentage,
        String discountName,
        String discountColorHex,
        BigDecimal loyaltyPrice,
        Integer loyaltyPercentage,
        boolean isPartialDiscount,
        List<TagView> tags,
        String productCode,
        List<SkuDto> skus) {

    /** Lightweight tag projection returned in listing responses. */
    public record TagView(Long id, String name, String colorHex) {}

    /**
     * Returns a copy of this DTO with loyalty pricing stamped.
     * Called by {@code TierPricingEnricher} for authenticated users who have a tier.
     *
     * <p>{@code loyaltyPercentage} is always the user's own tier %, regardless of
     * whether the sale discount is higher. {@code loyaltyPrice} uses whichever
     * percentage is greater — sale or tier — applied once to {@code originalPrice}.
     */
    public ListingVariantDto withLoyalty(BigDecimal loyaltyPct) {
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

        return new ListingVariantDto(productBaseId, variantId, slug, colorDisplayName, categoryName,
                colorKey, colorHex, webDescription, images,
                originalPrice, discountPrice, discountPercentage, discountName, discountColorHex,
                loyalty, loyaltyPct.intValue(), isPartialDiscount,
                tags, productCode, loyaltySkus);
    }
}
