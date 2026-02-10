package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full detail view for a single listing variant.
 * Includes the SKU list (sizes/prices) and sibling colour swatches.
 *
 * <p>Returned by {@code GET /api/v1/listings/{slug}}.
 */
public record ListingVariantDetailDto(
        Long id,
        String slug,
        String name,
        String colorKey,
        String webDescription,
        List<String> images,
        BigDecimal priceStart,
        BigDecimal priceEnd,
        Integer totalStock,
        List<SkuDto> skus,
        List<SiblingVariant> siblingVariants
) {
    /**
     * Lightweight reference to another colour variant of the same product.
     * Enables the frontend to render colour swatches without a second API call.
     */
    public record SiblingVariant(
            String slug,
            String colorKey,
            String thumbnail
    ) {}
}
