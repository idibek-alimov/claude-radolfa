package tj.radolfa.domain.model;

/**
 * Root of the product hierarchy — represents an Item Template from the
 * authoritative external catalogue.
 *
 * <p>
 * A ProductBase groups all colour variants and their SKUs under a single
 * external reference code. The {@code name} and {@code category} fields are
 * authoritative-source-locked and may only be updated through
 * {@link #applyExternalUpdate(String, String)}.
 *
 * <p>
 * Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class ProductBase {

    private final Long id;
    private final String externalRef;

    // Authoritative-source-locked fields
    private String name;
    private String category;

    // Radolfa-managed fields (never touched by ERP sync)
    private Long categoryId;
    private Long brandId;

    /**
     * @param id          database PK ({@code null} for unsaved instances)
     * @param externalRef required — external template identity, must not be blank
     * @param name        nullable — populated by import sync
     * @param category    nullable — populated by import sync (denormalized name)
     * @param categoryId  nullable — DB FK for the category; preferred over name for
     *                    persistence
     * @param brandId     nullable — Radolfa-managed, never overwritten by ERP sync
     */
    public ProductBase(Long id, String externalRef, String name, String category,
            Long categoryId, Long brandId) {
        if (externalRef == null || externalRef.isBlank()) {
            throw new IllegalArgumentException("externalRef must not be blank");
        }
        this.id = id;
        this.externalRef = externalRef;
        this.name = name;
        this.category = category;
        this.categoryId = categoryId;
        this.brandId = brandId;
    }

    /**
     * Authoritative-source merge — the ONLY path that writes the locked fields.
     */
    public void applyExternalUpdate(String name, String category) {
        this.name = name;
        this.category = category;
    }

    /**
     * Updates the product category (MANAGER / ADMIN action).
     * Both name and ID must be provided so the adapter can update the FK without
     * an extra round-trip.
     */
    public void updateCategory(String category, Long categoryId) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("categoryId must not be null");
        }
        this.category = category;
        this.categoryId = categoryId;
    }

    /**
     * Assigns a brand to this product. Radolfa-managed — never called from the ERP
     * sync path.
     */
    public void assignBrand(Long brandId) {
        this.brandId = brandId;
    }

    // ---- Getters (no setters — mutation is controlled) ----
    public Long getId() {
        return id;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getBrandId() {
        return brandId;
    }
}
