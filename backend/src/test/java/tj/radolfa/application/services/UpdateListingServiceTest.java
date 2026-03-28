package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.UpdateListingUseCase;
import tj.radolfa.application.ports.in.UpdateListingUseCase.UpdateListingCommand;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.SaveListingVariantPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UpdateListingService}.
 *
 * <p>Uses in-memory fakes — no Spring context, no Mockito.
 */
class UpdateListingServiceTest {

    private FakeLoadListingVariantPort fakeLoad;
    private FakeSaveListingVariantPort fakeSave;
    private UpdateListingService service;

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadListingVariantPort();
        fakeSave = new FakeSaveListingVariantPort();
        service  = new UpdateListingService(fakeLoad, fakeSave);
    }

    private ListingVariant storedVariant(String slug) {
        ListingVariant v = new ListingVariant(1L, 1L, "red", slug, null,
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null, null, false, true,
                null, null, null, null);
        fakeLoad.store(slug, v);
        return v;
    }

    // =========================================================
    //  update()
    // =========================================================

    @Test
    @DisplayName("update() throws ResourceNotFoundException for unknown slug")
    void update_unknownSlug_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.update("ghost-slug", new UpdateListingCommand(null, null)));
    }

    @Test
    @DisplayName("update() sets webDescription when provided")
    void update_setsWebDescription() {
        storedVariant("red-shirt");

        service.update("red-shirt", new UpdateListingCommand("Great red shirt", null));

        assertEquals("Great red shirt", fakeSave.lastSaved.getWebDescription());
    }

    @Test
    @DisplayName("update() does NOT change webDescription when field is null")
    void update_nullWebDescription_doesNotOverwrite() {
        ListingVariant existing = new ListingVariant(1L, 1L, "red", "red-shirt",
                "Original description", Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null, null, false, true,
                null, null, null, null);
        fakeLoad.store("red-shirt", existing);

        service.update("red-shirt", new UpdateListingCommand(null, null));

        assertEquals("Original description", fakeSave.lastSaved.getWebDescription());
    }

    @Test
    @DisplayName("update() replaces attributes list when provided")
    void update_replacesAttributes() {
        ListingVariant existing = new ListingVariant(1L, 1L, "red", "red-shirt",
                null, Collections.emptyList(),
                List.of(new ProductAttribute("OldKey", List.of("OldVal"), 0)),
                Collections.emptyList(), null, null, false, true,
                null, null, null, null);
        fakeLoad.store("red-shirt", existing);

        List<ProductAttribute> newAttrs = List.of(
                new ProductAttribute("Material", List.of("Silk"), 1),
                new ProductAttribute("Fit", List.of("Slim"), 2));
        service.update("red-shirt", new UpdateListingCommand(null, newAttrs));

        List<ProductAttribute> saved = fakeSave.lastSaved.getAttributes();
        assertEquals(2, saved.size());
        assertEquals("Material", saved.get(0).key());
        assertEquals("Fit", saved.get(1).key());
    }

    @Test
    @DisplayName("update() does NOT touch attributes when field is null")
    void update_nullAttributes_doesNotClearExisting() {
        ListingVariant existing = new ListingVariant(1L, 1L, "red", "red-shirt",
                null, Collections.emptyList(),
                List.of(new ProductAttribute("Fit", List.of("Regular"), 0)),
                Collections.emptyList(), null, null, false, true,
                null, null, null, null);
        fakeLoad.store("red-shirt", existing);

        service.update("red-shirt", new UpdateListingCommand(null, null));

        assertEquals(1, fakeSave.lastSaved.getAttributes().size());
    }

    @Test
    @DisplayName("update() replaces attributes with an empty list when empty list provided")
    void update_emptyAttributesList_clearsAttributes() {
        ListingVariant existing = new ListingVariant(1L, 1L, "red", "red-shirt",
                null, Collections.emptyList(),
                List.of(new ProductAttribute("Fit", List.of("Regular"), 0)),
                Collections.emptyList(), null, null, false, true,
                null, null, null, null);
        fakeLoad.store("red-shirt", existing);

        service.update("red-shirt", new UpdateListingCommand(null, List.of()));

        assertTrue(fakeSave.lastSaved.getAttributes().isEmpty());
    }

    @Test
    @DisplayName("update() always calls save exactly once")
    void update_savesExactlyOnce() {
        storedVariant("red-shirt");
        service.update("red-shirt", new UpdateListingCommand("desc", List.of()));
        assertEquals(1, fakeSave.saveCount);
    }

    // =========================================================
    //  addImage()
    // =========================================================

    @Test
    @DisplayName("addImage() throws ResourceNotFoundException for unknown slug")
    void addImage_unknownSlug_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.addImage("ghost", "https://cdn.example.com/img.jpg"));
    }

    @Test
    @DisplayName("addImage() appends URL to variant and persists")
    void addImage_appendsUrlAndSaves() {
        storedVariant("red-shirt");

        service.addImage("red-shirt", "https://cdn.example.com/img1.jpg");

        assertEquals(1, fakeSave.lastSaved.getImages().size());
        assertEquals("https://cdn.example.com/img1.jpg", fakeSave.lastSaved.getImages().get(0));
        assertEquals(1, fakeSave.saveCount);
    }

    @Test
    @DisplayName("addImage() with blank URL throws IllegalArgumentException (domain rule)")
    void addImage_blankUrl_throws() {
        storedVariant("red-shirt");

        assertThrows(IllegalArgumentException.class,
                () -> service.addImage("red-shirt", "   "));
    }

    // =========================================================
    //  removeImage()
    // =========================================================

    @Test
    @DisplayName("removeImage() throws ResourceNotFoundException for unknown slug")
    void removeImage_unknownSlug_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.removeImage("ghost", "https://cdn.example.com/img.jpg"));
    }

    @Test
    @DisplayName("removeImage() removes the URL and persists")
    void removeImage_removesUrlAndSaves() {
        ListingVariant existing = new ListingVariant(1L, 1L, "red", "red-shirt",
                null, new ArrayList<>(List.of("https://cdn.example.com/a.jpg")),
                Collections.emptyList(), Collections.emptyList(),
                null, null, false, true, null, null, null, null);
        fakeLoad.store("red-shirt", existing);

        service.removeImage("red-shirt", "https://cdn.example.com/a.jpg");

        assertTrue(fakeSave.lastSaved.getImages().isEmpty());
        assertEquals(1, fakeSave.saveCount);
    }

    @Test
    @DisplayName("removeImage() is a no-op for a URL that does not exist — still saves")
    void removeImage_missingUrl_stillSaves() {
        storedVariant("red-shirt");

        assertDoesNotThrow(() ->
                service.removeImage("red-shirt", "https://cdn.example.com/nonexistent.jpg"));
        assertEquals(1, fakeSave.saveCount);
    }

    // =========================================================
    //  In-memory fakes
    // =========================================================

    static class FakeLoadListingVariantPort implements LoadListingVariantPort {
        private final Map<String, ListingVariant> store = new HashMap<>();

        void store(String slug, ListingVariant v) { store.put(slug, v); }

        @Override
        public Optional<ListingVariant> findVariantById(Long id) { return Optional.empty(); }

        @Override
        public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long productBaseId, String colorKey) {
            return Optional.empty();
        }

        @Override
        public Optional<ListingVariant> findBySlug(String slug) {
            return Optional.ofNullable(store.get(slug));
        }

        @Override
        public List<ListingVariant> findAllByProductBaseId(Long productBaseId) {
            return List.of();
        }

        @Override
        public java.util.Map<Long, ListingVariant> findVariantsByIds(java.util.Collection<Long> ids) {
            return java.util.Map.of();
        }
    }

    static class FakeSaveListingVariantPort implements SaveListingVariantPort {
        int saveCount;
        ListingVariant lastSaved;

        @Override
        public void save(ListingVariant variant) {
            saveCount++;
            lastSaved = variant;
        }

        @Override
        public void saveTags(Long variantId, List<Long> tagIds) {
            // no-op in this test context
        }
    }
}
