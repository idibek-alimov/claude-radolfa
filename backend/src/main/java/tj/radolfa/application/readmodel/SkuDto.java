package tj.radolfa.application.readmodel;

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
        BigDecimal originalPrice,
        BigDecimal discountedPrice,
        BigDecimal loyaltyPrice,
        BigDecimal discountPercentage,
        BigDecimal loyaltyDiscountPercentage,
        String saleTitle,
        String saleColorHex,
        boolean onSale,
        Instant discountedEndsAt
) {
    public SkuDto withLoyaltyPrice(BigDecimal loyaltyPct) {
        BigDecimal effective = discountedPrice != null ? discountedPrice : originalPrice;
        if (effective == null || loyaltyPct.compareTo(BigDecimal.ZERO) == 0) return this;

        BigDecimal multiplier = BigDecimal.ONE.subtract(
                loyaltyPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal computed = effective.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        return new SkuDto(id, erpItemCode, sizeLabel, stockQuantity,
                originalPrice, discountedPrice, computed,
                discountPercentage, loyaltyPct, saleTitle, saleColorHex,
                onSale, discountedEndsAt);
    }
}
