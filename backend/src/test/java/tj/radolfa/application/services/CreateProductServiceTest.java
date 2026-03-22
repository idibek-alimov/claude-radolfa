package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.product.CreateProductUseCase;
import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.ports.out.LoadColorPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CreateProductService}.
 *
 * <p>Uses in-memory fakes (not Mockito). Verifies:
 * <ul>
 *   <li>Result contains productBaseId, variantId, and slug</li>
 *   <li>webDescription is propagated to the ListingVariant</li>
 *   <li>null webDescription still succeeds</li>
 * </ul>
 */
class CreateProductServiceTest {

    private CreateProductService service;
    private FakeLoadCategoryPort fakeCategoryPort;
    private FakeLoadColorPort fakeColorPort;
    private FakeSaveProductHierarchyPort fakeSavePort;
    private FakeListingIndexPort fakeIndexPort;

    @BeforeEach
    void setUp() {
        fakeCategoryPort = new FakeLoadCategoryPort();
        fakeColorPort = new FakeLoadColorPort();
        fakeSavePort = new FakeSaveProductHierarchyPort();
        fakeIndexPort = new FakeListingIndexPort();

        service = new CreateProductService(
                fakeCategoryPort,
                fakeColorPort,
                fakeSavePort,
                fakeIndexPort
        );
    }

    @Test
    @DisplayName("execute returns Result with productBaseId, variantId, and slug")
    void execute_returnsResultWithIds() {
        var command = new CreateProductUseCase.Command("Test Shirt", 1L, 1L, "A <b>bold</b> description", List.of(
                new CreateProductUseCase.Command.SkuDefinition("M", new Money(new BigDecimal("29.99")), 5)
        ), null);
        CreateProductUseCase.Result result = service.execute(command);
        assertNotNull(result.productBaseId());
        assertNotNull(result.variantId());
        assertNotNull(result.slug());
        assertTrue(result.slug().contains("test-shirt"));
    }

    @Test
    @DisplayName("execute propagates webDescription to ListingVariant")
    void execute_propagatesWebDescription() {
        var command = new CreateProductUseCase.Command("Test Shirt", 1L, 1L, "<p>Rich desc</p>", List.of(
                new CreateProductUseCase.Command.SkuDefinition("M", new Money(new BigDecimal("29.99")), 5)
        ), null);
        service.execute(command);
        ListingVariant saved = fakeSavePort.getLastSavedVariant();
        assertEquals("<p>Rich desc</p>", saved.getWebDescription());
    }

    @Test
    @DisplayName("execute with null webDescription still succeeds")
    void execute_nullWebDescription_succeeds() {
        var command = new CreateProductUseCase.Command("Test Shirt", 1L, 1L, null, List.of(
                new CreateProductUseCase.Command.SkuDefinition("M", new Money(new BigDecimal("29.99")), 5)
        ), null);
        CreateProductUseCase.Result result = service.execute(command);
        assertNotNull(result.productBaseId());
    }

    // ==== In-Memory Fakes ====

    static class FakeLoadCategoryPort implements LoadCategoryPort {

        @Override
        public Optional<CategoryView> findById(Long id) {
            return Optional.of(new CategoryView(id, "T-Shirts", "t-shirts", null));
        }

        @Override
        public Optional<CategoryView> findByName(String name) {
            return Optional.of(new CategoryView(1L, name, name.toLowerCase(), null));
        }

        @Override
        public Optional<CategoryView> findBySlug(String slug) {
            return Optional.of(new CategoryView(1L, "T-Shirts", slug, null));
        }

        @Override
        public List<CategoryView> findRoots() {
            return Collections.emptyList();
        }

        @Override
        public List<CategoryView> findByParentId(Long parentId) {
            return Collections.emptyList();
        }

        @Override
        public List<CategoryView> findAll() {
            return Collections.emptyList();
        }

        @Override
        public List<Long> getAllDescendantIds(Long categoryId) {
            return Collections.emptyList();
        }
    }

    static class FakeLoadColorPort implements LoadColorPort {

        @Override
        public Optional<ColorView> findByColorKey(String colorKey) {
            return Optional.of(new ColorView(1L, colorKey, "Red", "#FF0000"));
        }

        @Override
        public List<ColorView> findAll() {
            return Collections.emptyList();
        }

        @Override
        public Optional<ColorView> findById(Long id) {
            return Optional.of(new ColorView(id, "red", "Red", "#FF0000"));
        }
    }

    static class FakeSaveProductHierarchyPort implements SaveProductHierarchyPort {

        private final AtomicLong idGen = new AtomicLong(1);
        private ListingVariant lastSavedVariant;
        private Sku lastSavedSku;

        @Override
        public ProductBase saveBase(ProductBase base) {
            if (base.getId() != null) return base;
            return new ProductBase(idGen.getAndIncrement(), base.getExternalRef(), base.getName(), base.getCategory());
        }

        @Override
        public ListingVariant saveVariant(ListingVariant variant, Long productBaseId) {
            lastSavedVariant = variant;
            if (variant.getId() != null) return variant;
            return new ListingVariant(
                    idGen.getAndIncrement(),
                    productBaseId,
                    variant.getColorKey(),
                    variant.getSlug(),
                    variant.getWebDescription(),
                    variant.getImages(),
                    variant.getAttributes(),
                    variant.isTopSelling(),
                    variant.isFeatured(),
                    variant.isActive(),
                    variant.getLastSyncAt(),
                    null
            );
        }

        @Override
        public Sku saveSku(Sku sku, Long listingVariantId) {
            lastSavedSku = sku;
            if (sku.getId() != null) return sku;
            return new Sku(
                    idGen.getAndIncrement(),
                    listingVariantId,
                    sku.getSkuCode(),
                    sku.getSizeLabel(),
                    sku.getStockQuantity(),
                    sku.getPrice()
            );
        }

        public ListingVariant getLastSavedVariant() {
            return lastSavedVariant;
        }

        public Sku getLastSavedSku() {
            return lastSavedSku;
        }
    }

    static class FakeListingIndexPort implements ListingIndexPort {

        @Override
        public void index(Long variantId, String slug, String name, String category,
                          String colorKey, String colorHexCode,
                          String description, List<String> images,
                          Double price, Integer totalStock,
                          boolean topSelling, boolean featured, Instant lastSyncAt) {
            // no-op — fire-and-forget in production
        }

        @Override
        public void delete(String slug) {
            // no-op
        }
    }
}
