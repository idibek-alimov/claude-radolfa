package tj.radolfa.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductBaseTest {

    @Test
    @DisplayName("Constructor rejects null externalRef")
    void constructor_rejectsNullExternalRef() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductBase(null, null, "T-Shirt", "Clothing", null, null));
    }

    @Test
    @DisplayName("Constructor rejects blank externalRef")
    void constructor_rejectsBlankExternalRef() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductBase(null, "   ", "T-Shirt", "Clothing", null, null));
    }

    @Test
    @DisplayName("Constructor accepts valid args; getters return correct values")
    void constructor_happyPath() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", 3L, 10L);

        assertEquals(1L, base.getId());
        assertEquals("TPL-001", base.getExternalRef());
        assertEquals("T-Shirt", base.getName());
        assertEquals("Clothing", base.getCategory());
        assertEquals(3L, base.getCategoryId());
        assertEquals(10L, base.getBrandId());
    }

    @Test
    @DisplayName("Constructor accepts null name, category, categoryId, and brandId")
    void constructor_acceptsNullOptionalFields() {
        ProductBase base = new ProductBase(null, "TPL-002", null, null, null, null);

        assertNull(base.getName());
        assertNull(base.getCategory());
        assertNull(base.getCategoryId());
        assertNull(base.getBrandId());
    }

    @Test
    @DisplayName("applyExternalUpdate overwrites name and category")
    void applyExternalUpdate_overwritesNameAndCategory() {
        ProductBase base = new ProductBase(1L, "TPL-001", "Old Name", "Old Category", 2L, 5L);

        base.applyExternalUpdate("New Name", "New Category");

        assertEquals("New Name", base.getName());
        assertEquals("New Category", base.getCategory());
    }

    @Test
    @DisplayName("applyExternalUpdate does NOT touch brandId (sync boundary)")
    void applyExternalUpdate_doesNotTouchBrandId() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", 2L, 42L);

        base.applyExternalUpdate("T-Shirt v2", "Apparel");

        assertEquals(42L, base.getBrandId(), "brandId must not be overwritten by applyExternalUpdate");
    }

    @Test
    @DisplayName("assignBrand sets brandId")
    void assignBrand_setsBrandId() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", null, null);
        base.assignBrand(99L);

        assertEquals(99L, base.getBrandId());
    }

    @Test
    @DisplayName("updateCategory rejects null name")
    void updateCategory_rejectsNull() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", 2L, null);
        assertThrows(IllegalArgumentException.class, () -> base.updateCategory(null, 5L));
    }

    @Test
    @DisplayName("updateCategory rejects blank name")
    void updateCategory_rejectsBlank() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", 2L, null);
        assertThrows(IllegalArgumentException.class, () -> base.updateCategory("  ", 5L));
    }

    @Test
    @DisplayName("updateCategory rejects null categoryId")
    void updateCategory_rejectsNullCategoryId() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", 2L, null);
        assertThrows(IllegalArgumentException.class, () -> base.updateCategory("Sportswear", null));
    }

    @Test
    @DisplayName("updateCategory accepts valid name and categoryId")
    void updateCategory_happyPath() {
        ProductBase base = new ProductBase(1L, "TPL-001", "T-Shirt", "Clothing", 2L, null);
        base.updateCategory("Sportswear", 7L);

        assertEquals("Sportswear", base.getCategory());
        assertEquals(7L, base.getCategoryId());
    }
}
