package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Product card shown on the grid/listing page.
 * One card per colour variant (e.g. "T-Shirt — Red").
 *
 * <p>
 * Aggregate fields ({@code priceStart}, {@code priceEnd}, {@code totalStock})
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
                BigDecimal priceStart,
                BigDecimal priceEnd,
                BigDecimal tierPriceStart,
                BigDecimal tierPriceEnd,
                Integer totalStock,
                boolean topSelling,
                boolean featured) {

    public ListingVariantDto withTierDiscount(BigDecimal discountPercentage) {
        if (discountPercentage.compareTo(BigDecimal.ZERO) == 0) return this;

        BigDecimal multiplier = BigDecimal.ONE.subtract(
                discountPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        BigDecimal tpStart = priceStart != null
                ? priceStart.multiply(multiplier).setScale(2, RoundingMode.HALF_UP) : null;
        BigDecimal tpEnd = priceEnd != null
                ? priceEnd.multiply(multiplier).setScale(2, RoundingMode.HALF_UP) : null;

        return new ListingVariantDto(id, slug, name, category, colorKey, colorHexCode,
                webDescription, images, priceStart, priceEnd, tpStart, tpEnd,
                totalStock, topSelling, featured);
    }
}
