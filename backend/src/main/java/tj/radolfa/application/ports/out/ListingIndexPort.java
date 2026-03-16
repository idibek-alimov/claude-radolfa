package tj.radolfa.application.ports.out;

/**
 * Out-Port: index a product variant into the search engine.
 *
 * <p>Called after every ERP sync to keep the search index fresh.
 * Implementations must be fire-and-forget: failures are logged
 * but never propagate to the sync pipeline.
 */
public interface ListingIndexPort {

    /**
     * Upsert a product variant document into the search index.
     *
     * @param variantId    the ProductVariant id to index
     * @param slug         the variant slug
     * @param name         the ProductTemplate name (denormalized)
     * @param category     category name (may be null)
     * @param colorKey     the colour key from attributes (may be null)
     * @param colorHexCode hex code for the colour swatch (may be null)
     * @param description  template description (may be null)
     * @param images       image URLs from product_color_images
     * @param price        variant price (for search/sort)
     * @param totalStock   variant stock quantity
     * @param topSelling   whether the template is marked as top-selling
     * @param featured     whether the template is marked as featured
     */
    void index(Long variantId, String slug, String name, String category,
               String colorKey, String colorHexCode,
               String description, java.util.List<String> images,
               Double price, Integer totalStock,
               boolean topSelling, boolean featured, java.time.Instant lastSyncAt);

    /**
     * Remove a product variant from the search index.
     */
    void delete(String slug);
}
