package tj.radolfa.application.ports.out;

/**
 * Out-Port: index a listing variant into the search engine.
 *
 * <p>Implementations must be fire-and-forget: failures are logged
 * but never block the caller.
 */
public interface ListingIndexPort {

    /**
     * Upsert a listing variant document into the search index.
     *
     * @param variantId     the ListingVariant id to index
     * @param productBaseId the owning ProductBase id (used for Edit navigation)
     * @param slug          the variant slug
     * @param name          the ProductBase name (denormalized)
     * @param category      category name (may be null)
     * @param colorKey      the colour key
     * @param colorHexCode  hex code for the colour swatch (may be null)
     * @param description   web description (may be null)
     * @param images        image URLs
     * @param price         lowest effective price among SKUs (for search/sort)
     * @param totalStock    sum of all SKU stock
     * @param productCode   human-friendly product code, e.g. "RD-10047" (may be null)
     * @param skuCodes      all SKU codes belonging to this variant
     */
    void index(Long variantId, Long productBaseId, String slug, String name, String category,
               String colorKey, String colorHexCode,
               String description, java.util.List<String> images,
               Double price, Integer totalStock,
               java.time.Instant lastSyncAt,
               String productCode, java.util.List<String> skuCodes);

    /**
     * Remove a listing variant from the search index.
     */
    void delete(String slug);
}
