package tj.radolfa.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.domain.exception.IllegalProductStatusTransitionException;

import static org.junit.jupiter.api.Assertions.*;

class ProductBaseTest {

    @Test
    @DisplayName("Constructor rejects null externalRef")
    void constructor_rejectsNullExternalRef() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductBase(null, null, "T-Shirt", "Clothing", null, null, ProductStatus.DRAFT, null));
    }

    @Test
    @DisplayName("Constructor rejects blank externalRef")
    void constructor_rejectsBlankExternalRef() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductBase(null, "   ", "T-Shirt", "Clothing", null, null, ProductStatus.DRAFT, null));
    }

    @Test
    @DisplayName("Constructor accepts valid args; getters return correct values")
    void constructor_happyPath() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", 3L, 10L, ProductStatus.DRAFT, null);

        assertEquals(1L, base.getId());
        assertEquals("TPL-001", base.getExternalRef());
        assertEquals("T-Shirt", base.getName());
        assertEquals("Clothing", base.getCategory());
        assertEquals(3L, base.getCategoryId());
        assertEquals(10L, base.getBrandId());
        assertEquals(ProductStatus.DRAFT, base.getStatus());
        assertNull(base.getRejectionReason());
    }

    @Test
    @DisplayName("Constructor defaults null status to DRAFT")
    void constructor_defaultsNullStatusToDraft() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", null, null, null, null);
        assertEquals(ProductStatus.DRAFT, base.getStatus());
    }

    @Test
    @DisplayName("Constructor accepts null name, category, categoryId, and brandId")
    void constructor_acceptsNullOptionalFields() {
        ProductBase base = new ProductBase(null, "TPL-002", null, null, null, null, ProductStatus.DRAFT, null);

        assertNull(base.getName());
        assertNull(base.getCategory());
        assertNull(base.getCategoryId());
        assertNull(base.getBrandId());
    }

    @Test
    @DisplayName("applyExternalUpdate overwrites name and category")
    void applyExternalUpdate_overwritesNameAndCategory() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Old Name", "Old Category", 2L, 5L, ProductStatus.DRAFT, null);

        base.applyExternalUpdate("New Name", "New Category");

        assertEquals("New Name", base.getName());
        assertEquals("New Category", base.getCategory());
    }

    @Test
    @DisplayName("applyExternalUpdate does NOT touch brandId (sync boundary)")
    void applyExternalUpdate_doesNotTouchBrandId() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", 2L, 42L, ProductStatus.DRAFT, null);

        base.applyExternalUpdate("T-Shirt v2", "Apparel");

        assertEquals(42L, base.getBrandId(), "brandId must not be overwritten by applyExternalUpdate");
    }

    @Test
    @DisplayName("assignBrand sets brandId")
    void assignBrand_setsBrandId() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", null, null, ProductStatus.DRAFT, null);
        base.assignBrand(99L);

        assertEquals(99L, base.getBrandId());
    }

    @Test
    @DisplayName("updateCategory rejects null name")
    void updateCategory_rejectsNull() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", 2L, null, ProductStatus.DRAFT, null);
        assertThrows(IllegalArgumentException.class, () -> base.updateCategory(null, 5L));
    }

    @Test
    @DisplayName("updateCategory rejects blank name")
    void updateCategory_rejectsBlank() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", 2L, null, ProductStatus.DRAFT, null);
        assertThrows(IllegalArgumentException.class, () -> base.updateCategory("  ", 5L));
    }

    @Test
    @DisplayName("updateCategory rejects null categoryId")
    void updateCategory_rejectsNullCategoryId() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", 2L, null, ProductStatus.DRAFT, null);
        assertThrows(IllegalArgumentException.class, () -> base.updateCategory("Sportswear", null));
    }

    @Test
    @DisplayName("updateCategory accepts valid name and categoryId")
    void updateCategory_happyPath() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", 2L, null, ProductStatus.DRAFT, null);
        base.updateCategory("Sportswear", 7L);

        assertEquals("Sportswear", base.getCategory());
        assertEquals(7L, base.getCategoryId());
    }

    // ---- Lifecycle transitions ----

    @Test
    @DisplayName("submitForReview: DRAFT → PENDING_REVIEW, rejectionReason cleared")
    void submitForReview_fromDraft() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, ProductStatus.DRAFT, null);
        base.submitForReview();
        assertEquals(ProductStatus.PENDING_REVIEW, base.getStatus());
        assertNull(base.getRejectionReason());
    }

    @Test
    @DisplayName("submitForReview: REJECTED → PENDING_REVIEW, rejectionReason cleared")
    void submitForReview_fromRejected() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, ProductStatus.REJECTED, "bad image");
        base.submitForReview();
        assertEquals(ProductStatus.PENDING_REVIEW, base.getStatus());
        assertNull(base.getRejectionReason());
    }

    @Test
    @DisplayName("submitForReview from PENDING_REVIEW throws IllegalProductStatusTransitionException")
    void submitForReview_illegalFromPendingReview() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, ProductStatus.PENDING_REVIEW, null);
        assertThrows(IllegalProductStatusTransitionException.class, base::submitForReview);
    }

    @Test
    @DisplayName("submitForReview from ACTIVE throws IllegalProductStatusTransitionException")
    void submitForReview_illegalFromActive() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, ProductStatus.ACTIVE, null);
        assertThrows(IllegalProductStatusTransitionException.class, base::submitForReview);
    }

    @Test
    @DisplayName("approve: PENDING_REVIEW → AWAITING_STOCK")
    void approve_fromPendingReview() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, ProductStatus.PENDING_REVIEW, null);
        base.approve();
        assertEquals(ProductStatus.AWAITING_STOCK, base.getStatus());
    }

    @Test
    @DisplayName("approve from DRAFT throws IllegalProductStatusTransitionException")
    void approve_illegalFromDraft() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, ProductStatus.DRAFT, null);
        assertThrows(IllegalProductStatusTransitionException.class, base::approve);
    }

    @Test
    @DisplayName("reject: PENDING_REVIEW → REJECTED, stores reason")
    void reject_fromPendingReview() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, ProductStatus.PENDING_REVIEW, null);
        base.reject("Missing main image");
        assertEquals(ProductStatus.REJECTED, base.getStatus());
        assertEquals("Missing main image", base.getRejectionReason());
    }

    @Test
    @DisplayName("reject with blank reason throws IllegalArgumentException")
    void reject_blankReason() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, ProductStatus.PENDING_REVIEW, null);
        assertThrows(IllegalArgumentException.class, () -> base.reject("  "));
    }

    @Test
    @DisplayName("reject from DRAFT throws IllegalProductStatusTransitionException")
    void reject_illegalFromDraft() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, ProductStatus.DRAFT, null);
        assertThrows(IllegalProductStatusTransitionException.class, () -> base.reject("reason"));
    }

    @Test
    @DisplayName("activate: AWAITING_STOCK → ACTIVE")
    void activate_fromAwaitingStock() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, ProductStatus.AWAITING_STOCK, null);
        base.activate();
        assertEquals(ProductStatus.ACTIVE, base.getStatus());
    }

    @Test
    @DisplayName("activate from DRAFT throws IllegalProductStatusTransitionException")
    void activate_illegalFromDraft() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, ProductStatus.DRAFT, null);
        assertThrows(IllegalProductStatusTransitionException.class, base::activate);
    }

    @Test
    @DisplayName("resetToDraftOnEdit: PENDING_REVIEW → DRAFT")
    void resetToDraftOnEdit_fromPendingReview() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, ProductStatus.PENDING_REVIEW, null);
        base.resetToDraftOnEdit();
        assertEquals(ProductStatus.DRAFT, base.getStatus());
    }

    @Test
    @DisplayName("resetToDraftOnEdit: REJECTED → DRAFT")
    void resetToDraftOnEdit_fromRejected() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, ProductStatus.REJECTED, "bad image");
        base.resetToDraftOnEdit();
        assertEquals(ProductStatus.DRAFT, base.getStatus());
    }

    @Test
    @DisplayName("resetToDraftOnEdit is no-op for DRAFT, AWAITING_STOCK, ACTIVE")
    void resetToDraftOnEdit_noOpForOtherStates() {
        for (ProductStatus status : new ProductStatus[]{ProductStatus.DRAFT, ProductStatus.AWAITING_STOCK, ProductStatus.ACTIVE}) {
            ProductBase base = new ProductBase(1L, "TPL-001", "Shirt", "Clothing", 1L, null, status, null);
            base.resetToDraftOnEdit();
            assertEquals(status, base.getStatus(), "resetToDraftOnEdit must not change " + status);
        }
    }
}
