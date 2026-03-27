package tj.radolfa.application.readmodel;

import java.math.BigDecimal;

/**
 * Purchasable unit — one size of one colour variant.
 * Displayed on the product detail page as a size selector.
 *
 * <p>All pricing fields follow the same rules as {@link ListingVariantDto}:
 * {@code originalPrice} is always set; {@code discountPrice} is null when no
 * active sale applies to this SKU; {@code loyaltyPrice} is null for guests
 * and users without a loyalty tier.
 */
public record SkuDto(
        Long skuId,
        String skuCode,
        String sizeLabel,
        Integer stockQuantity,
        BigDecimal originalPrice,
        BigDecimal discountPrice,
        Integer discountPercentage,
        String discountName,
        String discountColorHex,
        BigDecimal loyaltyPrice
) {}
