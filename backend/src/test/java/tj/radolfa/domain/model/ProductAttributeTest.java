package tj.radolfa.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductAttributeTest {

    @Test
    @DisplayName("Constructor accepts valid key, value, and sortOrder")
    void constructor_happyPath() {
        ProductAttribute attr = new ProductAttribute("Material", "Cotton", 1);
        assertEquals("Material", attr.key());
        assertEquals("Cotton", attr.value());
        assertEquals(1, attr.sortOrder());
    }

    @Test
    @DisplayName("Constructor rejects null key")
    void constructor_rejectsNullKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductAttribute(null, "Cotton", 0));
    }

    @Test
    @DisplayName("Constructor rejects blank key")
    void constructor_rejectsBlankKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductAttribute("   ", "Cotton", 0));
    }

    @Test
    @DisplayName("Constructor rejects null value")
    void constructor_rejectsNullValue() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductAttribute("Material", null, 0));
    }

    @Test
    @DisplayName("Constructor rejects blank value")
    void constructor_rejectsBlankValue() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductAttribute("Material", "  ", 0));
    }

    @Test
    @DisplayName("Constructor accepts sortOrder = 0")
    void constructor_acceptsZeroSortOrder() {
        assertDoesNotThrow(() -> new ProductAttribute("Key", "Value", 0));
    }

    @Test
    @DisplayName("Two ProductAttribute records with the same fields are equal")
    void equality_byFields() {
        ProductAttribute a = new ProductAttribute("Fit", "Slim", 2);
        ProductAttribute b = new ProductAttribute("Fit", "Slim", 2);
        assertEquals(a, b);
    }
}
