package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
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
                Integer totalStock,
                boolean topSelling) {
}
