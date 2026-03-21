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
 *   <li>{@code minPrice} — effective lowest SKU price (sale discount already applied if active)
 *   <li>{@code maxPrice} — raw highest SKU price (use for strikethrough when minPrice differs)
 *   <li>{@code tierDiscountedMinPrice} — authenticated user's tier price (null for guests / no tier)
 * </ul>
 */
public record ListingVariantDto(
        Long variantId,
        String slug,
        String colorDisplayName,
        String categoryName,
        String colorKey,
        String colorHex,
        String webDescription,
        List<String> images,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal tierDiscountedMinPrice,
        boolean topSelling,
        boolean featured,
        String productCode,
        List<SkuDto> skus) {

    public ListingVariantDto withLoyaltyPrice(BigDecimal loyaltyPct) {
        if (loyaltyPct.compareTo(BigDecimal.ZERO) == 0) return this;

        BigDecimal multiplier = BigDecimal.ONE.subtract(
                loyaltyPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal tierPrice = minPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        return new ListingVariantDto(variantId, slug, colorDisplayName, categoryName,
                colorKey, colorHex, webDescription, images,
                minPrice, maxPrice, tierPrice,
                topSelling, featured, productCode, skus);
    }
}
