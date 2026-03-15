package tj.radolfa.infrastructure.web.dto;

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
                Integer totalStock,
                boolean topSelling,
                boolean featured) {

    public ListingVariantDto withLoyaltyPrice(BigDecimal discountPercentage) {
        if (discountPercentage.compareTo(BigDecimal.ZERO) == 0) return this;

        BigDecimal multiplier = BigDecimal.ONE.subtract(
                discountPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        BigDecimal base = discountedPrice != null ? discountedPrice : originalPrice;
        BigDecimal lp = base != null
                ? base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP) : null;

        return new ListingVariantDto(id, slug, name, category, colorKey, colorHexCode,
                webDescription, images, originalPrice, discountedPrice, lp,
                totalStock, topSelling, featured);
    }
}
