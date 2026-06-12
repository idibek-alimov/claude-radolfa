package tj.radolfa.domain.model;

import tj.radolfa.domain.exception.IllegalProductStatusTransitionException;

/**
 * Root of the product hierarchy — groups all colour variants and their SKUs
 * under a single reference code.
 *
 * <p>
 * The {@code name} and {@code category} fields are admin-managed and may only
 * be updated through {@link #applyExternalUpdate(String, String)}.
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

    // Radolfa-managed fields
    private Long categoryId;
    private Long brandId;

    // Marketplace ownership — null = Radolfa-owned
    private final Long sellerId;

    // Lifecycle
    private ProductStatus status;
    private String rejectionReason;

    /**
     * @param id              database PK ({@code null} for unsaved instances)
     * @param externalRef     required — external template identity, must not be blank
     * @param name            nullable — populated by import sync
     * @param category        nullable — populated by import sync (denormalized name)
     * @param categoryId      nullable — DB FK for the category
     * @param brandId         nullable — Radolfa-managed
     * @param status          nullable — defaults to DRAFT when null
     * @param rejectionReason nullable — set only when status is REJECTED
     * @param sellerId        nullable — {@code null} means Radolfa-owned (marketplace Phase 2+)
     */
    public ProductBase(Long id, String externalRef, String name, String category,
            Long categoryId, Long brandId,
            ProductStatus status, String rejectionReason, Long sellerId) {
        if (externalRef == null || externalRef.isBlank()) {
            throw new IllegalArgumentException("externalRef must not be blank");
        }
        this.id = id;
        this.externalRef = externalRef;
        this.name = name;
        this.category = category;
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.status = status != null ? status : ProductStatus.DRAFT;
        this.rejectionReason = rejectionReason;
        this.sellerId = sellerId;
    }

    /**
     * Convenience constructor for Radolfa-owned products ({@code sellerId = null}).
     * All existing call sites use this form — unchanged for backward compatibility.
     */
    public ProductBase(Long id, String externalRef, String name, String category,
            Long categoryId, Long brandId,
            ProductStatus status, String rejectionReason) {
        this(id, externalRef, name, category, categoryId, brandId, status, rejectionReason, null);
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
     * Assigns a brand to this product. Radolfa-managed.
     */
    public void assignBrand(Long brandId) {
        this.brandId = brandId;
    }

    // ---- Lifecycle transitions ----

    /** DRAFT or REJECTED → PENDING_REVIEW. Clears any previous rejection reason. */
    public void submitForReview() {
        if (status != ProductStatus.DRAFT && status != ProductStatus.REJECTED) {
            throw new IllegalProductStatusTransitionException(status, ProductStatus.PENDING_REVIEW);
        }
        this.status = ProductStatus.PENDING_REVIEW;
        this.rejectionReason = null;
    }

    /** PENDING_REVIEW → AWAITING_STOCK (approval collapses into the awaiting-stock state). */
    public void approve() {
        if (status != ProductStatus.PENDING_REVIEW) {
            throw new IllegalProductStatusTransitionException(status, ProductStatus.AWAITING_STOCK);
        }
        this.status = ProductStatus.AWAITING_STOCK;
    }

    /** PENDING_REVIEW → REJECTED. Requires a non-blank reason. */
    public void reject(String reason) {
        if (status != ProductStatus.PENDING_REVIEW) {
            throw new IllegalProductStatusTransitionException(status, ProductStatus.REJECTED);
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("rejection reason must not be blank");
        }
        this.status = ProductStatus.REJECTED;
        this.rejectionReason = reason;
    }

    /** AWAITING_STOCK → ACTIVE. Called by the first putaway hook; irreversible. */
    public void activate() {
        if (status != ProductStatus.AWAITING_STOCK) {
            throw new IllegalProductStatusTransitionException(status, ProductStatus.ACTIVE);
        }
        this.status = ProductStatus.ACTIVE;
    }

    /**
     * Resets PENDING_REVIEW or REJECTED to DRAFT when any edit is made.
     * Idempotent — no-op for DRAFT, AWAITING_STOCK, and ACTIVE.
     */
    public void resetToDraftOnEdit() {
        if (status == ProductStatus.PENDING_REVIEW || status == ProductStatus.REJECTED) {
            this.status = ProductStatus.DRAFT;
        }
    }

    // ---- Getters (no setters — mutation is controlled) ----

    public Long getId() { return id; }

    public String getExternalRef() { return externalRef; }

    public String getName() { return name; }

    public String getCategory() { return category; }

    public Long getCategoryId() { return categoryId; }

    public Long getBrandId() { return brandId; }

    public ProductStatus getStatus() { return status; }

    public String getRejectionReason() { return rejectionReason; }

    /** {@code null} means the product is owned by Radolfa (not a marketplace seller). */
    public Long getSellerId() { return sellerId; }
}
