package tj.radolfa.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Root aggregate — represents an ERPNext Item template (has_variants=1)
 * or a standalone item (no variants).
 *
 * <p>ERP-locked fields: {@code name}, {@code category}.
 * Enrichment fields: {@code description}, {@code topSelling}, {@code featured}.
 *
 * <p>{@code attributesDefinition} describes which attributes exist and their
 * possible values, e.g. {@code {"Color": ["Red", "Blue"], "Size": ["S", "M"]}}.
 * Standalone items have an empty map.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class ProductTemplate {

    private final Long   id;
    private final String erpTemplateCode;

    // ERP-locked fields
    private String name;
    private String category;

    // Enrichment fields — owned by Radolfa content team
    private String description;
    private boolean topSelling;
    private boolean featured;

    // Attribute schema — populated from ERPNext variant attributes
    private Map<String, List<String>> attributesDefinition;

    private boolean active;

    public ProductTemplate(Long id,
                           String erpTemplateCode,
                           String name,
                           String category,
                           String description,
                           Map<String, List<String>> attributesDefinition,
                           boolean active,
                           boolean topSelling,
                           boolean featured) {
        if (erpTemplateCode == null || erpTemplateCode.isBlank()) {
            throw new IllegalArgumentException("erpTemplateCode must not be blank");
        }
        this.id                   = id;
        this.erpTemplateCode      = erpTemplateCode;
        this.name                 = name;
        this.category             = category;
        this.description          = description;
        this.attributesDefinition = attributesDefinition != null
                ? new LinkedHashMap<>(attributesDefinition)
                : new LinkedHashMap<>();
        this.active               = active;
        this.topSelling           = topSelling;
        this.featured             = featured;
    }

    /**
     * ERP merge — the ONLY path that writes the locked fields.
     */
    public void updateFromErp(String name, String category, boolean disabled) {
        this.name     = name;
        this.category = category;
        this.active   = !disabled;
    }

    /**
     * Updates the attribute schema from ERP variant data.
     */
    public void updateAttributesDefinition(Map<String, List<String>> attributesDefinition) {
        this.attributesDefinition = attributesDefinition != null
                ? new LinkedHashMap<>(attributesDefinition)
                : new LinkedHashMap<>();
    }

    // ---- Enrichment mutations (Radolfa-owned) ----

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateTopSelling(boolean topSelling) {
        this.topSelling = topSelling;
    }

    public void updateFeatured(boolean featured) {
        this.featured = featured;
    }

    // ---- Getters ----
    public Long                      getId()                   { return id; }
    public String                    getErpTemplateCode()      { return erpTemplateCode; }
    public String                    getName()                 { return name; }
    public String                    getCategory()             { return category; }
    public String                    getDescription()          { return description; }
    public Map<String, List<String>> getAttributesDefinition() { return Collections.unmodifiableMap(attributesDefinition); }
    public boolean                   isActive()                { return active; }
    public boolean                   isTopSelling()            { return topSelling; }
    public boolean                   isFeatured()              { return featured; }
}
