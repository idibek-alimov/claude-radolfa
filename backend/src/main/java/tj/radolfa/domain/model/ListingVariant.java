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

    // Audit
    private Instant lastSyncAt;

    public ListingVariant(Long id,
                          Long productBaseId,
                          String colorKey,
                          String slug,
                          String webDescription,
                          List<String> images,
                          Instant lastSyncAt) {
        this.id             = id;
        this.productBaseId  = productBaseId;
        this.colorKey       = colorKey;
        this.slug           = slug;
        this.webDescription = webDescription;
        this.images         = new ArrayList<>(images != null ? images : List.of());
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

    public void addImage(String url) {
        this.images.add(url);
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
    public Instant      getLastSyncAt()     { return lastSyncAt; }
}
