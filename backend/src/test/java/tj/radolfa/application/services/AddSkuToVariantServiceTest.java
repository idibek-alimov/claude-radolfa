package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.product.AddSkuToVariantUseCase.Command;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AddSkuToVariantService}.
 *
 * <p>No Spring context, no Mockito — hand-written in-memory fake adapters.
 */
class AddSkuToVariantServiceTest {

    private static final Long PRODUCT_BASE_ID = 1L;
    private static final Long VARIANT_ID = 10L;

    private FakeLoadListingVariantPort fakeLoad;
    private FakeSaveHierarchyPort      fakeSave;
    private AddSkuToVariantService     service;

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadListingVariantPort();
        fakeSave = new FakeSaveHierarchyPort();
        service  = new AddSkuToVariantService(fakeLoad, fakeSave);

        // Store a valid variant for the happy-path tests
        fakeLoad.store(buildVariant(VARIANT_ID, PRODUCT_BASE_ID));
    }

    // =========================================================
    //  Happy-path
    // =========================================================

    @Test
    @DisplayName("Returns generated SKU id on success")
    void execute_returnsSkuId_whenVariantExists() {
        Command cmd = new Command(PRODUCT_BASE_ID, VARIANT_ID, "XL", new Money(new BigDecimal("49.99")), 10);

        Long skuId = service.execute(cmd);

        assertNotNull(skuId);
    }

    @Test
    @DisplayName("Auto-generates skuCode with 'SKU-' prefix")
    void execute_autoGeneratesSkuCode() {
        service.execute(new Command(PRODUCT_BASE_ID, VARIANT_ID, "M", new Money(new BigDecimal("29.99")), 5));

        assertNotNull(fakeSave.lastSavedSku);
        assertTrue(fakeSave.lastSavedSku.getSkuCode().startsWith("SKU-"),
                "skuCode must start with 'SKU-'");
    }

    @Test
    @DisplayName("Auto-generates barcode with 'BC-' prefix")
    void execute_autoGeneratesBarcode() {
        service.execute(new Command(PRODUCT_BASE_ID, VARIANT_ID, "S", new Money(new BigDecimal("19.99")), 3));

        assertNotNull(fakeSave.lastSavedSku.getBarcode());
        assertTrue(fakeSave.lastSavedSku.getBarcode().startsWith("BC-"),
                "barcode must start with 'BC-'");
    }

    @Test
    @DisplayName("Saved SKU has the correct sizeLabel, price, and stock")
    void execute_savedSkuHasCorrectFields() {
        Money price = new Money(new BigDecimal("99.00"));
        service.execute(new Command(PRODUCT_BASE_ID, VARIANT_ID, "XXL", price, 7));

        Sku sku = fakeSave.lastSavedSku;
        assertEquals("XXL", sku.getSizeLabel());
        assertEquals(new BigDecimal("99.00"), sku.getPrice().amount());
        assertEquals(7, sku.getStockQuantity());
    }

    // =========================================================
    //  Error paths
    // =========================================================

    @Test
    @DisplayName("Throws ResourceNotFoundException when variant does not exist")
    void execute_throwsResourceNotFoundException_whenVariantMissing() {
        Command cmd = new Command(PRODUCT_BASE_ID, 999L, "M", new Money(new BigDecimal("29.99")), 5);

        assertThrows(ResourceNotFoundException.class, () -> service.execute(cmd));
        assertNull(fakeSave.lastSavedSku, "No SKU must be saved when variant is missing");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when variant belongs to a different product")
    void execute_throwsResourceNotFoundException_whenVariantOwnershipMismatch() {
        // Variant 10 belongs to productBaseId=1, but the command claims productBaseId=99
        Command cmd = new Command(99L, VARIANT_ID, "M", new Money(new BigDecimal("29.99")), 5);

        assertThrows(ResourceNotFoundException.class, () -> service.execute(cmd));
        assertNull(fakeSave.lastSavedSku, "No SKU must be saved on ownership mismatch");
    }

    // =========================================================
    //  Fake helpers
    // =========================================================

    private static ListingVariant buildVariant(Long id, Long productBaseId) {
        return new ListingVariant(
                id, productBaseId, "red", "test-slug", null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                null, "RD-00001", true, true, null, null, null, null);
    }

    static class FakeLoadListingVariantPort implements LoadListingVariantPort {
        private final Map<Long, ListingVariant> store = new HashMap<>();

        void store(ListingVariant v) { store.put(v.getId(), v); }

        @Override
        public Optional<ListingVariant> findVariantById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long productBaseId, String colorKey) {
            return Optional.empty();
        }

        @Override
        public Optional<ListingVariant> findBySlug(String slug) { return Optional.empty(); }

        @Override
        public List<ListingVariant> findAllByProductBaseId(Long productBaseId) { return List.of(); }

        @Override
        public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) { return Map.of(); }
    }

    static class FakeSaveHierarchyPort implements SaveProductHierarchyPort {
        private final AtomicLong idGen = new AtomicLong(100);
        Sku lastSavedSku;

        @Override
        public ProductBase saveBase(ProductBase base) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListingVariant saveVariant(ListingVariant variant, Long productBaseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Sku saveSku(Sku sku, Long listingVariantId) {
            lastSavedSku = new Sku(
                    idGen.getAndIncrement(),
                    listingVariantId,
                    sku.getSkuCode(),
                    sku.getSizeLabel(),
                    sku.getStockQuantity(),
                    sku.getPrice(),
                    sku.getBarcode());
            return lastSavedSku;
        }
    }
}
