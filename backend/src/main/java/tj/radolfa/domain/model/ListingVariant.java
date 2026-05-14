package tj.radolfa.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A colour variant of a {@link ProductBase} — the unit displayed on the
 * storefront product-list page.
 *
 * <p>
 * The {@code webDescription} and {@code images} are <b>enrichment</b>
 * fields owned by the Radolfa content team and must <b>never</b>
 * be overwritten once they have been populated.
 *
 * <p>
 * Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class ListingVariant {

    private final Long id;
    private final Long productBaseId;
    private final String colorKey;
    private String slug;

    // Enrichment fields — Radolfa-managed, never overwritten
    private String webDescription;
    private List<String> images;
    private List<ProductAttribute> attributes;
    private List<Long> tagIds;

    // Logistics dimensions — Radolfa-owned, one profile per colour variant
    private Double  weightKg;
    private Integer widthCm;
    private Integer heightCm;
    private Integer depthCm;

    // Visibility lifecycle flags
    private boolean isEnabled;
    private boolean isActive;

    // Audit
    private Instant lastSyncAt;

    // Human-friendly storefront identifier (e.g. "RD-10047").
    // Null for unsaved variants — assigned by the persistence layer on first save.
    // Never changes after initial assignment.
    private String productCode;

    public ListingVariant(Long id,
            Long productBaseId,
            String colorKey,
            String slug,
            String webDescription,
            List<String> images,
            List<ProductAttribute> attributes,
            List<Long> tagIds,
            Instant lastSyncAt,
            String productCode,
            boolean isEnabled,
            boolean isActive,
            Double weightKg,
            Integer widthCm,
            Integer heightCm,
            Integer depthCm) {
        this.id = id;
        this.productBaseId = productBaseId;
        this.colorKey = colorKey;
        this.slug = slug;
        this.webDescription = webDescription;
        this.images = new ArrayList<>(images != null ? images : List.of());
        this.attributes = new ArrayList<>(attributes != null ? attributes : List.of());
        this.tagIds = new ArrayList<>(tagIds != null ? tagIds : List.of());
        this.lastSyncAt = lastSyncAt;
        this.productCode = productCode;
        this.isEnabled = isEnabled;
        this.isActive = isActive;
        this.weightKg = weightKg;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.depthCm = depthCm;
    }

    /**
     * Generates a URL-safe slug from the template code and colour key.
     * Called once during creation; idempotent on subsequent syncs.
     */
    public void generateSlug(String templateCode) {
        if (this.slug == null || this.slug.isBlank()) {
            this.slug = (templateCode + "-" + colorKey)
                    .toLowerCase()
                    .replaceAll("[^a-z0-9-]", "-")
                    .replaceAll("-+", "-");
        }
    }

    /**
     * Stamps the sync clock. Does NOT touch enrichment fields.
     */
    public void markSynced() {
        this.lastSyncAt = Instant.now();
    }

    // ---- Enrichment mutations (Radolfa-owned) ----

    public void updateWebDescription(String webDescription) {
        this.webDescription = webDescription;
    }

    public void setAttributes(List<ProductAttribute> attributes) {
        this.attributes = new ArrayList<>(attributes != null ? attributes : List.of());
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

    public void assignTags(List<Long> tagIds) {
        this.tagIds = new ArrayList<>(tagIds != null ? tagIds : List.of());
    }

    public void updateDimensions(Double weightKg, Integer widthCm, Integer heightCm, Integer depthCm) {
        this.weightKg = weightKg;
        this.widthCm  = widthCm;
        this.heightCm = heightCm;
        this.depthCm  = depthCm;
    }

    public void updateIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void updateIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    // ---- Queries ----

    public boolean hasEnrichment() {
        return (webDescription != null && !webDescription.isBlank())
                || !images.isEmpty();
    }

    // ---- Getters ----
    public Long getId() {
        return id;
    }

    public Long getProductBaseId() {
        return productBaseId;
    }

    public String getColorKey() {
        return colorKey;
    }

    public String getSlug() {
        return slug;
    }

    public String getWebDescription() {
        return webDescription;
    }

    public List<String> getImages() {
        return Collections.unmodifiableList(images);
    }

    public List<ProductAttribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    public List<Long> getTagIds() {
        return Collections.unmodifiableList(tagIds);
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public String getProductCode() {
        return productCode;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isActive() {
        return isActive;
    }

    public Double  getWeightKg() { return weightKg; }
    public Integer getWidthCm()  { return widthCm; }
    public Integer getHeightCm() { return heightCm; }
    public Integer getDepthCm()  { return depthCm; }
}
