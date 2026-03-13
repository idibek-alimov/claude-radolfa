package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Purchasable unit — one size of one colour variant.
 * Displayed on the product detail page as a size selector.
 */
public record SkuDto(
        Long id,
        String erpItemCode,
        String sizeLabel,
        Integer stockQuantity,
        BigDecimal price,
        BigDecimal salePrice,
        BigDecimal tierPrice,
        boolean onSale,
        Instant saleEndsAt
) {
    public SkuDto withTierDiscount(BigDecimal discountPercentage) {
        BigDecimal effective = salePrice != null ? salePrice : price;
        if (effective == null || discountPercentage.compareTo(BigDecimal.ZERO) == 0) return this;

        BigDecimal multiplier = BigDecimal.ONE.subtract(
                discountPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal computed = effective.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        return new SkuDto(id, erpItemCode, sizeLabel, stockQuantity,
                price, salePrice, computed, onSale, saleEndsAt);
    }
}
