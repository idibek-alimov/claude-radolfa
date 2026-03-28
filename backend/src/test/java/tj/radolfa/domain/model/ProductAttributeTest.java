package tj.radolfa.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductAttributeTest {

    @Test
    @DisplayName("Constructor accepts valid key, values list, and sortOrder")
    void constructor_happyPath() {
        ProductAttribute attr = new ProductAttribute("Material", List.of("Cotton"), 1);
        assertEquals("Material", attr.key());
        assertEquals(List.of("Cotton"), attr.values());
        assertEquals(1, attr.sortOrder());
    }

    @Test
    @DisplayName("Constructor rejects null key")
    void constructor_rejectsNullKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductAttribute(null, List.of("Cotton"), 0));
    }

    @Test
    @DisplayName("Constructor rejects blank key")
    void constructor_rejectsBlankKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductAttribute("   ", List.of("Cotton"), 0));
    }

    @Test
    @DisplayName("Constructor rejects null values list")
    void constructor_rejectsNullValuesList() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductAttribute("Material", null, 0));
    }

    @Test
    @DisplayName("Constructor rejects empty values list")
    void constructor_rejectsEmptyValuesList() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductAttribute("Material", List.of(), 0));
    }

    @Test
    @DisplayName("Constructor rejects blank value inside list")
    void constructor_rejectsBlankValueInList() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductAttribute("Material", List.of("  "), 0));
    }

    @Test
    @DisplayName("Constructor accepts sortOrder = 0")
    void constructor_acceptsZeroSortOrder() {
        assertDoesNotThrow(() -> new ProductAttribute("Key", List.of("Value"), 0));
    }

    @Test
    @DisplayName("Constructor accepts multiple values")
    void constructor_acceptsMultipleValues() {
        ProductAttribute attr = new ProductAttribute("Material", List.of("Cotton", "Polyester"), 0);
        assertEquals(2, attr.values().size());
    }

    @Test
    @DisplayName("Two ProductAttribute records with the same fields are equal")
    void equality_byFields() {
        ProductAttribute a = new ProductAttribute("Fit", List.of("Slim"), 2);
        ProductAttribute b = new ProductAttribute("Fit", List.of("Slim"), 2);
        assertEquals(a, b);
    }
}
