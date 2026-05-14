package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.product.AddVariantToProductUseCase.Command;
import tj.radolfa.application.ports.in.product.AddVariantToProductUseCase.Result;
import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.LoadColorPort;
import tj.radolfa.application.ports.out.LoadColorPort.ColorView;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.domain.exception.DuplicateResourceException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AddVariantToProductService}.
 *
 * <p>No Spring context, no Mockito — hand-written in-memory fake adapters.
 */
class AddVariantToProductServiceTest {

    private FakeLoadProductBasePort     fakeBase;
    private FakeLoadColorPort           fakeColor;
    private FakeLoadListingVariantPort  fakeVariant;
    private FakeSaveHierarchyPort       fakeSave;
    private FakeListingIndexPort        fakeIndex;
    private AddVariantToProductService  service;

    @BeforeEach
    void setUp() {
        fakeBase    = new FakeLoadProductBasePort();
        fakeColor   = new FakeLoadColorPort();
        fakeVariant = new FakeLoadListingVariantPort();
        fakeSave    = new FakeSaveHierarchyPort();
        fakeIndex   = new FakeListingIndexPort();
        service     = new AddVariantToProductService(fakeBase, fakeColor, fakeVariant, fakeSave, fakeIndex);

        // Default fixtures
        fakeBase.store(new ProductBase(1L, "INTERNAL-ABC123", "Winter Jacket", "Clothing", 1L, null));
        fakeColor.store(new ColorView(10L, "red", "Red", "#FF0000"));
    }

    // =========================================================
    //  Happy-path
    // =========================================================

    @Test
    @DisplayName("Returns variantId and slug on success")
    void execute_returnsResult_whenValid() {
        Result result = service.execute(new Command(1L, 10L));

        assertNotNull(result.variantId());
        assertNotNull(result.slug());
        assertFalse(result.slug().isBlank());
    }

    @Test
    @DisplayName("Generated slug is URL-safe (only lowercase letters, digits, hyphens)")
    void execute_slug_isUrlSafe() {
        Result result = service.execute(new Command(1L, 10L));

        assertTrue(result.slug().matches("[a-z0-9-]+"),
                "Slug must be URL-safe: " + result.slug());
    }

    @Test
    @DisplayName("ES indexing is called once (fire-and-forget)")
    void execute_esIndex_calledOnce() {
        service.execute(new Command(1L, 10L));

        assertEquals(1, fakeIndex.indexCallCount);
    }

    @Test
    @DisplayName("ES indexing failure does not propagate — variant is still saved")
    void execute_esIndexFailure_doesNotRollback() {
        fakeIndex.throwOnIndex = true;

        Result result = service.execute(new Command(1L, 10L));

        assertNotNull(result.variantId(), "variantId must be returned even when ES indexing throws");
        assertEquals(1, fakeSave.variantSaveCount);
    }

    // =========================================================
    //  Error paths
    // =========================================================

    @Test
    @DisplayName("Throws ResourceNotFoundException when ProductBase does not exist")
    void execute_throwsResourceNotFoundException_whenBaseMissing() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.execute(new Command(999L, 10L)));
        assertEquals(0, fakeSave.variantSaveCount);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when Color does not exist")
    void execute_throwsResourceNotFoundException_whenColorMissing() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.execute(new Command(1L, 999L)));
        assertEquals(0, fakeSave.variantSaveCount);
    }

    @Test
    @DisplayName("Throws DuplicateResourceException when the color already exists on the base")
    void execute_throwsDuplicateResourceException_whenColorAlreadyUsed() {
        // Pre-store a variant with colorKey "red" on productBase 1
        fakeVariant.storeForBaseAndColor(1L, "red",
                new ListingVariant(50L, 1L, "red", "internal-abc123-red", null,
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                        null, "RD-00050", false, true, null, null, null, null));

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> service.execute(new Command(1L, 10L)));

        assertTrue(ex.getMessage().contains("red"));
        assertEquals(0, fakeSave.variantSaveCount);
    }

    // =========================================================
    //  Fakes
    // =========================================================

    static class FakeLoadProductBasePort implements LoadProductBasePort {
        private final Map<Long, ProductBase> store = new HashMap<>();

        void store(ProductBase b) { store.put(b.getId(), b); }

        @Override
        public Optional<ProductBase> findById(Long id) { return Optional.ofNullable(store.get(id)); }

        @Override
        public Optional<ProductBase> findByExternalRef(String externalRef) { return Optional.empty(); }
    }

    static class FakeLoadColorPort implements LoadColorPort {
        private final Map<Long, ColorView> store = new HashMap<>();

        void store(ColorView v) { store.put(v.id(), v); }

        @Override
        public Optional<ColorView> findById(Long id) { return Optional.ofNullable(store.get(id)); }

        @Override
        public Optional<ColorView> findByColorKey(String colorKey) { return Optional.empty(); }

        @Override
        public List<ColorView> findAll() { return List.copyOf(store.values()); }
    }

    static class FakeLoadListingVariantPort implements LoadListingVariantPort {
        // key: "baseId:colorKey" → variant
        private final Map<String, ListingVariant> byBaseAndColor = new HashMap<>();

        void storeForBaseAndColor(Long baseId, String colorKey, ListingVariant v) {
            byBaseAndColor.put(baseId + ":" + colorKey, v);
        }

        @Override
        public Optional<ListingVariant> findVariantById(Long id) { return Optional.empty(); }

        @Override
        public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long productBaseId, String colorKey) {
            return Optional.ofNullable(byBaseAndColor.get(productBaseId + ":" + colorKey));
        }

        @Override
        public Optional<ListingVariant> findBySlug(String slug) { return Optional.empty(); }

        @Override
        public List<ListingVariant> findAllByProductBaseId(Long productBaseId) { return List.of(); }

        @Override
        public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) { return Map.of(); }
    }

    static class FakeSaveHierarchyPort implements SaveProductHierarchyPort {
        private final AtomicLong idGen = new AtomicLong(200);
        int variantSaveCount;

        @Override
        public ProductBase saveBase(ProductBase base) { throw new UnsupportedOperationException(); }

        @Override
        public ListingVariant saveVariant(ListingVariant variant, Long productBaseId) {
            variantSaveCount++;
            return new ListingVariant(
                    idGen.getAndIncrement(), productBaseId, variant.getColorKey(),
                    variant.getSlug(), null,
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                    null, "RD-" + idGen.get(),
                    variant.isEnabled(), variant.isActive(),
                    null, null, null, null);
        }

        @Override
        public Sku saveSku(Sku sku, Long listingVariantId) { throw new UnsupportedOperationException(); }
    }

    static class FakeListingIndexPort implements ListingIndexPort {
        int indexCallCount;
        boolean throwOnIndex;

        @Override
        public void index(Long variantId, Long productBaseId, String slug, String name,
                          String category, String colorKey, String colorHexCode,
                          String description, List<String> images,
                          Double price, Integer totalStock,
                          Instant lastSyncAt, String productCode, List<String> skuCodes) {
            if (throwOnIndex) throw new RuntimeException("ES unavailable");
            indexCallCount++;
        }

        @Override
        public void delete(String slug) {}
    }
}
