package tj.radolfa.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListingVariantTest {

    private ListingVariant freshVariant() {
        return new ListingVariant(null, 1L, "red", null, null,
                Collections.emptyList(), Collections.emptyList(),
                false, false, null, null);
    }

    // ---- slug generation ----

    @Test
    @DisplayName("generateSlug builds slug from externalRef + colorKey")
    void generateSlug_buildsCorrectSlug() {
        ListingVariant v = freshVariant();
        v.generateSlug("INTERNAL-ABCD1234");

        assertEquals("internal-abcd1234-red", v.getSlug());
    }

    @Test
    @DisplayName("generateSlug sanitises special characters to hyphens")
    void generateSlug_sanitisesSpecialChars() {
        ListingVariant v = new ListingVariant(null, 1L, "dark blue", null, null,
                Collections.emptyList(), Collections.emptyList(),
                false, false, null, null);
        v.generateSlug("TPL 001");

        // spaces become hyphens; consecutive hyphens collapsed
        assertFalse(v.getSlug().contains(" "), "Slug must not contain spaces");
        assertFalse(v.getSlug().contains("--"), "Slug must not contain consecutive hyphens");
    }

    @Test
    @DisplayName("generateSlug is idempotent — does not overwrite existing slug")
    void generateSlug_idempotent() {
        ListingVariant v = new ListingVariant(1L, 1L, "red", "existing-slug", null,
                Collections.emptyList(), Collections.emptyList(),
                false, false, null, null);
        v.generateSlug("INTERNAL-NEW");

        assertEquals("existing-slug", v.getSlug(), "generateSlug must not overwrite an existing slug");
    }

    // ---- webDescription ----

    @Test
    @DisplayName("updateWebDescription sets webDescription")
    void updateWebDescription_setsValue() {
        ListingVariant v = freshVariant();
        v.updateWebDescription("Beautiful red shirt");

        assertEquals("Beautiful red shirt", v.getWebDescription());
    }

    // ---- attributes ----

    @Test
    @DisplayName("setAttributes replaces attribute list")
    void setAttributes_replacesExistingList() {
        ListingVariant v = freshVariant();
        v.setAttributes(List.of(new ProductAttribute("Material", "Cotton", 1)));
        v.setAttributes(List.of(
                new ProductAttribute("Fit", "Slim", 1),
                new ProductAttribute("Care", "Machine wash", 2)));

        assertEquals(2, v.getAttributes().size());
        assertEquals("Fit", v.getAttributes().get(0).key());
    }

    @Test
    @DisplayName("setAttributes(null) results in empty list")
    void setAttributes_nullYieldsEmptyList() {
        ListingVariant v = freshVariant();
        v.setAttributes(null);

        assertTrue(v.getAttributes().isEmpty());
    }

    @Test
    @DisplayName("getAttributes returns unmodifiable view")
    void getAttributes_returnsUnmodifiableView() {
        ListingVariant v = freshVariant();
        v.setAttributes(List.of(new ProductAttribute("Key", "Val", 0)));

        assertThrows(UnsupportedOperationException.class,
                () -> v.getAttributes().add(new ProductAttribute("X", "Y", 99)));
    }

    // ---- images ----

    @Test
    @DisplayName("addImage appends URL to images list")
    void addImage_appendsUrl() {
        ListingVariant v = freshVariant();
        v.addImage("https://cdn.example.com/img1.jpg");
        v.addImage("https://cdn.example.com/img2.jpg");

        assertEquals(2, v.getImages().size());
        assertEquals("https://cdn.example.com/img1.jpg", v.getImages().get(0));
    }

    @Test
    @DisplayName("addImage rejects null URL")
    void addImage_rejectsNull() {
        ListingVariant v = freshVariant();
        assertThrows(IllegalArgumentException.class, () -> v.addImage(null));
    }

    @Test
    @DisplayName("addImage rejects blank URL")
    void addImage_rejectsBlank() {
        ListingVariant v = freshVariant();
        assertThrows(IllegalArgumentException.class, () -> v.addImage("   "));
    }

    @Test
    @DisplayName("addImage enforces MAX_IMAGES = 20 limit")
    void addImage_enforcesMaxLimit() {
        ListingVariant v = freshVariant();
        for (int i = 1; i <= 20; i++) {
            v.addImage("https://cdn.example.com/img" + i + ".jpg");
        }

        assertThrows(IllegalStateException.class,
                () -> v.addImage("https://cdn.example.com/img21.jpg"),
                "Should throw when adding the 21st image");
    }

    @Test
    @DisplayName("removeImage removes the URL from the list")
    void removeImage_removesUrl() {
        ListingVariant v = freshVariant();
        v.addImage("https://cdn.example.com/a.jpg");
        v.addImage("https://cdn.example.com/b.jpg");
        v.removeImage("https://cdn.example.com/a.jpg");

        assertEquals(1, v.getImages().size());
        assertEquals("https://cdn.example.com/b.jpg", v.getImages().get(0));
    }

    @Test
    @DisplayName("removeImage is a no-op for a URL that does not exist")
    void removeImage_noOpForMissingUrl() {
        ListingVariant v = freshVariant();
        v.addImage("https://cdn.example.com/a.jpg");

        assertDoesNotThrow(() -> v.removeImage("https://cdn.example.com/nonexistent.jpg"));
        assertEquals(1, v.getImages().size());
    }

    @Test
    @DisplayName("getImages returns unmodifiable view")
    void getImages_returnsUnmodifiableView() {
        ListingVariant v = freshVariant();
        v.addImage("https://cdn.example.com/a.jpg");

        assertThrows(UnsupportedOperationException.class,
                () -> v.getImages().add("https://cdn.example.com/b.jpg"));
    }

    // ---- flags ----

    @Test
    @DisplayName("updateTopSelling and updateFeatured toggle flags")
    void updateFlags_toggleCorrectly() {
        ListingVariant v = freshVariant();
        assertFalse(v.isTopSelling());
        assertFalse(v.isFeatured());

        v.updateTopSelling(true);
        v.updateFeatured(true);

        assertTrue(v.isTopSelling());
        assertTrue(v.isFeatured());
    }

    // ---- hasEnrichment ----

    @Test
    @DisplayName("hasEnrichment returns false on fresh variant")
    void hasEnrichment_falseOnFreshVariant() {
        assertFalse(freshVariant().hasEnrichment());
    }

    @Test
    @DisplayName("hasEnrichment returns true when webDescription is set")
    void hasEnrichment_trueWhenDescriptionSet() {
        ListingVariant v = freshVariant();
        v.updateWebDescription("Great product");
        assertTrue(v.hasEnrichment());
    }

    @Test
    @DisplayName("hasEnrichment returns true when at least one image exists")
    void hasEnrichment_trueWhenImageExists() {
        ListingVariant v = freshVariant();
        v.addImage("https://cdn.example.com/img.jpg");
        assertTrue(v.hasEnrichment());
    }

    // ---- constructor initialises from provided lists ----

    @Test
    @DisplayName("Constructor makes a defensive copy of the provided images list")
    void constructor_makesDefensiveCopyOfImages() {
        List<String> images = new ArrayList<>();
        images.add("https://cdn.example.com/a.jpg");
        ListingVariant v = new ListingVariant(null, 1L, "blue", null, null,
                images, Collections.emptyList(), false, false, null, null);

        images.add("https://cdn.example.com/b.jpg"); // mutate original

        assertEquals(1, v.getImages().size(), "Variant must not be affected by external list mutation");
    }
}
