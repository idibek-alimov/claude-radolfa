package tj.radolfa.application.readmodel;

import java.math.BigDecimal;

/**
 * Purchasable unit — one size of one colour variant.
 * Displayed on the product detail page as a size selector.
 *
 * <p>
 * {@code price} is the effective price after any active sale discount.
 * Tier (loyalty) pricing is applied at the variant level via
 * {@code tierDiscountedMinPrice}, not per-SKU.
 */
public record SkuDto(
        Long skuId,
        String skuCode,
        String sizeLabel,
        Integer stockQuantity,
        BigDecimal price
) {}
