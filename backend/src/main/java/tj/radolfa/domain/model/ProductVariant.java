package tj.radolfa.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A purchasable variant — maps 1:1 to an ERPNext Item Variant
 * (or the single implicit variant of a standalone item).
 *
 * <p>ERP-locked fields: {@code price}, {@code stockQty}, {@code active}.
 * These are always overwritten on sync via {@link #updateFromErp}.
 *
 * <p>Enrichment fields: {@code seoSlug}.
 * Images are now owned by {@link ColorImages} at the (template, color) level.
 *
 * <p>{@code attributes} holds the concrete attribute values for this variant,
 * e.g. {@code {"Color": "Red", "Size": "M"}}. Standalone items have an empty map.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class ProductVariant {

    private final Long   id;
    private final Long   templateId;
    private final String erpVariantCode;

    // Attribute values for this specific variant
    private Map<String, String> attributes;

    // ERP-locked fields — always overwritten
    private Money   price;
    private Integer stockQty;
    private boolean active;

    // Enrichment fields — never overwritten by ERP sync
    private String seoSlug;

    // Audit
    private Instant lastSyncAt;

    public ProductVariant(Long id,
                          Long templateId,
                          String erpVariantCode,
                          Map<String, String> attributes,
                          Money price,
                          Integer stockQty,
                          boolean active,
                          String seoSlug,
                          Instant lastSyncAt) {
        if (erpVariantCode == null || erpVariantCode.isBlank()) {
            throw new IllegalArgumentException("erpVariantCode must not be blank");
        }
        this.id             = id;
        this.templateId     = templateId;
        this.erpVariantCode = erpVariantCode;
        this.attributes     = attributes != null ? new LinkedHashMap<>(attributes) : new LinkedHashMap<>();
        this.price          = price;
        this.stockQty       = stockQty;
        this.active         = active;
        this.seoSlug        = seoSlug;
        this.lastSyncAt     = lastSyncAt;
    }

    /**
     * ERP merge — overwrites ALL pricing, stock, and status fields.
     */
    public void updateFromErp(Money price, Integer stockQty, boolean disabled) {
        this.price    = price;
        this.stockQty = stockQty;
        this.active   = !disabled;
    }

    /**
     * Updates attribute values (called during sync when ERP sends variant attributes).
     */
    public void updateAttributes(Map<String, String> attributes) {
        this.attributes = attributes != null ? new LinkedHashMap<>(attributes) : new LinkedHashMap<>();
    }

    /**
     * Stamps the sync clock. Called on every successful ERP sync.
     */
    public void markSynced() {
        this.lastSyncAt = Instant.now();
    }

    /**
     * Generates a URL-safe slug from the template code and color attribute.
     * Called once during creation; idempotent on subsequent syncs.
     */
    public void generateSlug(String erpTemplateCode) {
        if (this.seoSlug == null || this.seoSlug.isBlank()) {
            String color = attributes.getOrDefault("Color", "default");
            this.seoSlug = (erpTemplateCode + "-" + color)
                    .toLowerCase()
                    .replaceAll("[^a-z0-9-]", "-")
                    .replaceAll("-+", "-");
        }
    }

    // ---- Queries ----

    public String getColor() {
        return attributes.getOrDefault("Color", null);
    }

    public String getSize() {
        return attributes.getOrDefault("Size", null);
    }

    // ---- Getters ----
    public Long                getId()             { return id; }
    public Long                getTemplateId()     { return templateId; }
    public String              getErpVariantCode() { return erpVariantCode; }
    public Map<String, String> getAttributes()     { return Collections.unmodifiableMap(attributes); }
    public Money               getPrice()          { return price; }
    public Integer             getStockQty()       { return stockQty; }
    public boolean             isActive()          { return active; }
    public String              getSeoSlug()        { return seoSlug; }
    public Instant             getLastSyncAt()     { return lastSyncAt; }
}
