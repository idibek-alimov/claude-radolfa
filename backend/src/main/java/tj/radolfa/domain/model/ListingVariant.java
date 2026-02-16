package tj.radolfa.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A colour variant of a {@link ProductBase} — the unit displayed on the
 * storefront product-list page.
 *
 * <p>The {@code webDescription} and {@code images} are <b>enrichment</b>
 * fields owned by the Radolfa content team. ERP sync must <b>never</b>
 * overwrite them once they have been populated.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class ListingVariant {

    private final Long   id;
    private final Long   productBaseId;
    private final String colorKey;
    private String       slug;

    // Enrichment fields — never overwritten by ERP sync
    private String       webDescription;
    private List<String> images;
    private boolean      topSelling;
    private boolean      featured;

    // Audit
    private Instant lastSyncAt;

    public ListingVariant(Long id,
                          Long productBaseId,
                          String colorKey,
                          String slug,
                          String webDescription,
                          List<String> images,
                          boolean topSelling,
                          boolean featured,
                          Instant lastSyncAt) {
        this.id             = id;
        this.productBaseId  = productBaseId;
        this.colorKey       = colorKey;
        this.slug           = slug;
        this.webDescription = webDescription;
        this.images         = new ArrayList<>(images != null ? images : List.of());
        this.topSelling     = topSelling;
        this.featured       = featured;
        this.lastSyncAt     = lastSyncAt;
    }

    /**
     * Generates a URL-safe slug from the template code and colour key.
     * Called once during creation; idempotent on subsequent syncs.
     */
    public void generateSlug(String erpTemplateCode) {
        if (this.slug == null || this.slug.isBlank()) {
            this.slug = (erpTemplateCode + "-" + colorKey)
                    .toLowerCase()
                    .replaceAll("[^a-z0-9-]", "-")
                    .replaceAll("-+", "-");
        }
    }

    /**
     * Stamps the sync clock. Called on every successful ERP sync.
     * Does NOT touch enrichment fields.
     */
    public void markSynced() {
        this.lastSyncAt = Instant.now();
    }

    // ---- Enrichment mutations (Radolfa-owned) ----

    public void updateWebDescription(String webDescription) {
        this.webDescription = webDescription;
    }

    private static final int MAX_IMAGES = 20;

    public void addImage(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Image URL must not be blank");
        }
        if (this.images.size() >= MAX_IMAGES) {
            throw new IllegalStateException("Maximum of " + MAX_IMAGES + " images reached");
        }
        this.images.add(url);
    }

    public void removeImage(String url) {
        this.images.remove(url);
    }

    public void updateTopSelling(boolean topSelling) {
        this.topSelling = topSelling;
    }

    public void updateFeatured(boolean featured) {
        this.featured = featured;
    }

    // ---- Queries ----

    public boolean hasEnrichment() {
        return (webDescription != null && !webDescription.isBlank())
                || !images.isEmpty();
    }

    // ---- Getters ----
    public Long         getId()             { return id; }
    public Long         getProductBaseId()  { return productBaseId; }
    public String       getColorKey()       { return colorKey; }
    public String       getSlug()           { return slug; }
    public String       getWebDescription() { return webDescription; }
    public List<String> getImages()         { return Collections.unmodifiableList(images); }
    public boolean      isTopSelling()      { return topSelling; }
    public boolean      isFeatured()        { return featured; }
    public Instant      getLastSyncAt()     { return lastSyncAt; }
}
