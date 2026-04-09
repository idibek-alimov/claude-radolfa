package tj.radolfa.application.readmodel;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full admin read model for a product card.
 * Returned by {@code GET /api/v1/admin/products/{productBaseId}}.
 *
 * <p>Intentionally lean — no discount/loyalty fields. The admin edit screen
 * works with raw data only; price enrichment is the public listing's concern.
 */
public record ProductCardDto(
        Long productBaseId,
        String name,
        String brand,
        Long categoryId,
        String categoryName,
        List<VariantSummary> variants
) {

    /**
     * One color variant of the product card, with its images, attributes, tags, and SKUs.
     */
    public record VariantSummary(
            Long variantId,
            String slug,
            String productCode,
            Long colorId,
            String colorKey,
            String colorDisplayName,
            String colorHex,
            String webDescription,
            List<ImageRef> images,
            List<AttributeDto> attributes,
            List<TagView> tags,
            List<SkuSummary> skus,
            boolean isEnabled,
            boolean isActive,
            Double weightKg,
            Integer widthCm,
            Integer heightCm,
            Integer depthCm
    ) {}

    /**
     * A single SKU (size entry) in the admin context.
     * Price shown as raw {@code originalPrice}; no computed discount/loyalty fields.
     */
    public record SkuSummary(
            Long skuId,
            String skuCode,
            String sizeLabel,
            Integer stockQuantity,
            BigDecimal originalPrice
    ) {}

    /** Image reference exposing the database ID alongside the URL, needed for sort-order updates. */
    public record ImageRef(Long id, String url) {}

    /** Key/values attribute pair (e.g. key="Material", values=["Cotton","Acrylic"]). */
    public record AttributeDto(String key, List<String> values) {}

    /** Lightweight tag reference. */
    public record TagView(Long id, String name, String colorHex) {}
}
