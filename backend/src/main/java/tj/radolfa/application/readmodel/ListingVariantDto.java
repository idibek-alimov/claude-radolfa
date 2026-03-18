package tj.radolfa.application.readmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Product card shown on the grid/listing page.
 * One card per colour variant (e.g. "T-Shirt — Red").
 *
 * <p>
 * Aggregate fields ({@code originalPrice}, {@code totalStock})
 * are computed from the variant's SKUs so the grid never needs to fetch
 * individual sizes.
 */
public record ListingVariantDto(
                Long id,
                String slug,
                String name,
                String category,
                String colorKey,
                String colorHexCode,
                String webDescription,
                List<String> images,
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
                String productCode) {

    public ListingVariantDto withLoyaltyPrice(BigDecimal loyaltyPct) {
        if (loyaltyPct.compareTo(BigDecimal.ZERO) == 0) return this;

        BigDecimal base = discountedPrice != null ? discountedPrice : originalPrice;
        if (base == null) return this;

        BigDecimal multiplier = BigDecimal.ONE.subtract(
                loyaltyPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal lp = base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        return new ListingVariantDto(id, slug, name, category, colorKey, colorHexCode,
                webDescription, images, originalPrice, discountedPrice, lp,
                discountPercentage, loyaltyPct, saleTitle, saleColorHex,
                totalStock, topSelling, featured, productCode);
    }
}
