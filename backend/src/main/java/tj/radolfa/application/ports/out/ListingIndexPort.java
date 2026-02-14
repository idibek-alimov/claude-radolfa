package tj.radolfa.application.ports.out;

/**
 * Out-Port: index a listing variant into the search engine.
 *
 * <p>Called after every ERP sync to keep the search index fresh.
 * Implementations must be fire-and-forget: failures are logged
 * but never propagate to the sync pipeline.
 */
public interface ListingIndexPort {

    /**
     * Upsert a listing variant document into the search index.
     *
     * @param variantId    the ListingVariant id to index
     * @param slug         the variant slug
     * @param name         the ProductBase name (denormalized)
     * @param category     category name (may be null)
     * @param colorKey     the colour key
     * @param colorHexCode hex code for the colour swatch (may be null)
     * @param description  web description (may be null)
     * @param images       image URLs
     * @param priceStart   lowest sale price among SKUs
     * @param priceEnd     highest sale price among SKUs
     * @param totalStock   sum of all SKU stock
     * @param topSelling   whether this variant is marked as top-selling
     */
    void index(Long variantId, String slug, String name, String category,
               String colorKey, String colorHexCode,
               String description, java.util.List<String> images,
               Double priceStart, Double priceEnd, Integer totalStock,
               boolean topSelling, java.time.Instant lastSyncAt);

    /**
     * Remove a listing variant from the search index.
     */
    void delete(String slug);
}
