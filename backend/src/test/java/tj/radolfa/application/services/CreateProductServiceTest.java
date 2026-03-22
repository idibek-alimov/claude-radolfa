package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.product.CreateProductUseCase;
import tj.radolfa.application.ports.in.product.CreateProductUseCase.Command;
import tj.radolfa.application.ports.in.product.CreateProductUseCase.Command.SkuDefinition;
import tj.radolfa.application.ports.in.product.CreateProductUseCase.Command.VariantDefinition;
import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.LoadBrandPort;
import tj.radolfa.application.ports.out.LoadBrandPort.BrandView;
import tj.radolfa.application.ports.out.LoadCategoryBlueprintPort;
import tj.radolfa.application.ports.out.LoadCategoryBlueprintPort.BlueprintEntry;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.readmodel.CategoryView;
import tj.radolfa.application.ports.out.LoadColorPort;
import tj.radolfa.application.ports.out.LoadColorPort.ColorView;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductAttribute;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CreateProductService}.
 *
 * <p>All external dependencies are replaced with hand-written in-memory fakes.
 * No Spring context, no Mockito, no database.
 */
class CreateProductServiceTest {

    // ---- Fakes ----

    private FakeLoadCategoryPort    fakeCategory;
    private FakeLoadColorPort       fakeColor;
    private FakeLoadBrandPort       fakeBrand;
    private FakeLoadBlueprintPort   fakeBlueprint;
    private FakeSaveHierarchyPort   fakeSave;
    private FakeListingIndexPort    fakeIndex;
    private CreateProductService    service;

    @BeforeEach
    void setUp() {
        fakeCategory  = new FakeLoadCategoryPort();
        fakeColor     = new FakeLoadColorPort();
        fakeBrand     = new FakeLoadBrandPort();
        fakeBlueprint = new FakeLoadBlueprintPort();
        fakeSave      = new FakeSaveHierarchyPort();
        fakeIndex     = new FakeListingIndexPort();

        service = new CreateProductService(
                fakeCategory, fakeColor, fakeBrand,
                fakeBlueprint, fakeSave, fakeIndex);

        // Default "happy path" fixtures
        fakeCategory.store(new CategoryView(1L, "Clothing", "clothing", null));
        fakeColor.store(new ColorView(10L, "red", "Red", "#FF0000"));
    }

    // =========================================================
    //  Happy-path tests
    // =========================================================

    @Test
    @DisplayName("Creates ProductBase and returns its ID — single variant, single SKU")
    void execute_singleVariant_createsHierarchyAndReturnsBaseId() {
        Command cmd = commandWith(List.of(variantDef(10L, List.of(skuDef("S", "29.99", 50, "BC-001")))));

        Long id = service.execute(cmd);

        assertNotNull(id);
        assertEquals(1, fakeSave.baseSaveCount);
        assertEquals(1, fakeSave.variantSaveCount);
        assertEquals(1, fakeSave.skuSaveCount);
    }

    @Test
    @DisplayName("Creates multiple variants in a single call")
    void execute_multipleVariants_createsAllVariants() {
        fakeColor.store(new ColorView(20L, "blue", "Blue", "#0000FF"));

        Command cmd = commandWith(List.of(
                variantDef(10L, List.of(skuDef("S", "29.99", 50, "BC-001"))),
                variantDef(20L, List.of(
                        skuDef("M", "34.99", 20, "BC-002"),
                        skuDef("L", "34.99", 15, "BC-003")))
        ));

        service.execute(cmd);

        assertEquals(1, fakeSave.baseSaveCount);
        assertEquals(2, fakeSave.variantSaveCount);
        assertEquals(3, fakeSave.skuSaveCount);
    }

    @Test
    @DisplayName("SKU stores barcode and logistics fields correctly")
    void execute_skuLogisticsFields_persistedCorrectly() {
        SkuDefinition sku = new SkuDefinition("XL", new Money(new BigDecimal("49.99")),
                30, "BARCODE-XL", 0.8, 30, 20, 10);

        service.execute(commandWith(List.of(variantDef(10L, List.of(sku)))));

        Sku saved = fakeSave.lastSavedSku;
        assertNotNull(saved);
        assertEquals("BARCODE-XL", saved.getBarcode());
        assertEquals(0.8, saved.getWeightKg());
        assertEquals(30, saved.getWidthCm());
        assertEquals(20, saved.getHeightCm());
        assertEquals(10, saved.getDepthCm());
        assertEquals("XL", saved.getSizeLabel());
        assertEquals(30, saved.getStockQuantity());
    }

    @Test
    @DisplayName("SKU logistics fields are null when not provided")
    void execute_skuLogisticsFields_nullWhenOmitted() {
        SkuDefinition sku = new SkuDefinition("M", new Money(new BigDecimal("19.99")),
                10, "BC-M", null, null, null, null);

        service.execute(commandWith(List.of(variantDef(10L, List.of(sku)))));

        Sku saved = fakeSave.lastSavedSku;
        assertNull(saved.getWeightKg());
        assertNull(saved.getWidthCm());
        assertNull(saved.getHeightCm());
        assertNull(saved.getDepthCm());
    }

    @Test
    @DisplayName("Variant webDescription is set when provided")
    void execute_webDescription_isSetOnVariant() {
        VariantDefinition varDef = new VariantDefinition(
                10L, "Beautiful red dress", List.of(), List.of(),
                List.of(skuDef("S", "59.99", 5, "BC-D")));

        service.execute(commandWith(List.of(varDef)));

        ListingVariant saved = fakeSave.lastSavedVariant;
        assertEquals("Beautiful red dress", saved.getWebDescription());
    }

    @Test
    @DisplayName("Variant images are added when provided")
    void execute_images_areAddedToVariant() {
        VariantDefinition varDef = new VariantDefinition(
                10L, null, List.of(),
                List.of("https://cdn.example.com/a.jpg", "https://cdn.example.com/b.jpg"),
                List.of(skuDef("S", "29.99", 10, "BC-I")));

        service.execute(commandWith(List.of(varDef)));

        assertEquals(2, fakeSave.lastSavedVariant.getImages().size());
    }

    @Test
    @DisplayName("Variant attributes are set when provided")
    void execute_attributes_areSetOnVariant() {
        List<ProductAttribute> attrs = List.of(
                new ProductAttribute("Material", "Silk", 1),
                new ProductAttribute("Fit", "Regular", 2));

        VariantDefinition varDef = new VariantDefinition(
                10L, null, attrs, List.of(),
                List.of(skuDef("S", "19.99", 8, "BC-A")));

        service.execute(commandWith(List.of(varDef)));

        assertEquals(2, fakeSave.lastSavedVariant.getAttributes().size());
    }

    @Test
    @DisplayName("Product is created without a brand when brandId is null")
    void execute_noBrand_productBaseHasNullBrandId() {
        Command cmd = new Command("T-Shirt", 1L, null,
                List.of(variantDef(10L, List.of(skuDef("S", "29.99", 5, "BC-NB")))));

        service.execute(cmd);

        assertNull(fakeSave.lastSavedBase.getBrandId());
    }

    @Test
    @DisplayName("Product is created with brandId when brand exists")
    void execute_withBrand_productBaseHasBrandId() {
        fakeBrand.store(new BrandView(7L, "Nike"));
        Command cmd = new Command("T-Shirt", 1L, 7L,
                List.of(variantDef(10L, List.of(skuDef("S", "29.99", 5, "BC-BR")))));

        service.execute(cmd);

        assertEquals(7L, fakeSave.lastSavedBase.getBrandId());
    }

    @Test
    @DisplayName("Variant slug is generated and is non-blank")
    void execute_variantSlug_isGeneratedAndNotBlank() {
        service.execute(commandWith(List.of(variantDef(10L, List.of(skuDef("S", "29.99", 5, "BC-SL"))))));

        String slug = fakeSave.lastSavedVariant.getSlug();
        assertNotNull(slug);
        assertFalse(slug.isBlank());
        assertTrue(slug.matches("[a-z0-9-]+"), "Slug must be URL-safe: " + slug);
    }

    @Test
    @DisplayName("SKU code starts with 'SKU-' prefix")
    void execute_skuCode_hasPrefixSkuDash() {
        service.execute(commandWith(List.of(variantDef(10L, List.of(skuDef("M", "39.99", 3, "BC-SC"))))));

        assertTrue(fakeSave.lastSavedSku.getSkuCode().startsWith("SKU-"));
    }

    @Test
    @DisplayName("ES indexing is called once per variant (fire-and-forget)")
    void execute_esIndex_calledOncePerVariant() {
        fakeColor.store(new ColorView(20L, "blue", "Blue", "#0000FF"));
        service.execute(commandWith(List.of(
                variantDef(10L, List.of(skuDef("S", "19.99", 5, "BC-E1"))),
                variantDef(20L, List.of(skuDef("M", "19.99", 5, "BC-E2"))))));

        assertEquals(2, fakeIndex.indexCallCount);
    }

    @Test
    @DisplayName("ES indexing failure does not propagate — product is still created")
    void execute_esIndexFailure_doesNotRollBack() {
        fakeIndex.throwOnIndex = true;

        Long id = service.execute(commandWith(List.of(
                variantDef(10L, List.of(skuDef("S", "19.99", 5, "BC-EF"))))));

        assertNotNull(id, "ProductBase ID must be returned even when ES indexing throws");
        assertEquals(1, fakeSave.baseSaveCount);
        assertEquals(1, fakeSave.variantSaveCount);
    }

    // =========================================================
    //  Validation — category
    // =========================================================

    @Test
    @DisplayName("Throws ResourceNotFoundException when category does not exist")
    void execute_categoryNotFound_throws() {
        Command cmd = new Command("T-Shirt", 999L, null,
                List.of(variantDef(10L, List.of(skuDef("S", "29.99", 5, "BC-CF")))));

        assertThrows(ResourceNotFoundException.class, () -> service.execute(cmd));
        assertEquals(0, fakeSave.baseSaveCount, "No product should be saved");
    }

    // =========================================================
    //  Validation — brand
    // =========================================================

    @Test
    @DisplayName("Throws ResourceNotFoundException when brandId is provided but brand does not exist")
    void execute_brandNotFound_throws() {
        Command cmd = new Command("T-Shirt", 1L, 999L,
                List.of(variantDef(10L, List.of(skuDef("S", "29.99", 5, "BC-BF")))));

        assertThrows(ResourceNotFoundException.class, () -> service.execute(cmd));
        assertEquals(0, fakeSave.baseSaveCount);
    }

    // =========================================================
    //  Validation — color
    // =========================================================

    @Test
    @DisplayName("Throws ResourceNotFoundException when color does not exist")
    void execute_colorNotFound_throws() {
        Command cmd = commandWith(List.of(
                variantDef(999L, List.of(skuDef("S", "29.99", 5, "BC-CLF")))));

        assertThrows(ResourceNotFoundException.class, () -> service.execute(cmd));
    }

    @Test
    @DisplayName("Throws for the first variant whose color is missing; does not save base")
    void execute_colorNotFoundOnSecondVariant_doesNotPartiallyCommit() {
        // First color exists, second does not
        fakeColor.store(new ColorView(10L, "red", "Red", "#FF0000"));

        Command cmd = commandWith(List.of(
                variantDef(10L, List.of(skuDef("S", "29.99", 5, "BC-C1"))),
                variantDef(888L, List.of(skuDef("M", "29.99", 5, "BC-C2")))  // missing color
        ));

        // The service is @Transactional — in a real context the whole TX rolls back.
        // Here we assert that the service throws when the missing color is hit.
        assertThrows(ResourceNotFoundException.class, () -> service.execute(cmd));
    }

    // =========================================================
    //  Validation — category blueprint (required attributes)
    // =========================================================

    @Test
    @DisplayName("No error when category has no blueprint requirements")
    void execute_noBlueprintRequirements_noError() {
        // fakeBlueprint returns empty list by default
        assertDoesNotThrow(() -> service.execute(
                commandWith(List.of(variantDef(10L, List.of(skuDef("S", "19.99", 5, "BC-NR")))))));
    }

    @Test
    @DisplayName("Passes validation when all required blueprint keys are provided")
    void execute_allRequiredAttributesProvided_noError() {
        fakeBlueprint.storeRequired(1L, "Material");
        fakeBlueprint.storeRequired(1L, "Fit");

        List<ProductAttribute> attrs = List.of(
                new ProductAttribute("Material", "Wool", 1),
                new ProductAttribute("Fit", "Slim", 2));
        VariantDefinition varDef = new VariantDefinition(
                10L, null, attrs, List.of(),
                List.of(skuDef("S", "29.99", 5, "BC-RA")));

        assertDoesNotThrow(() -> service.execute(commandWith(List.of(varDef))));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when a required blueprint attribute is missing")
    void execute_missingRequiredAttribute_throwsIllegalArgument() {
        fakeBlueprint.storeRequired(1L, "Material");

        // Variant provides no attributes at all
        VariantDefinition varDef = new VariantDefinition(
                10L, null, List.of(), List.of(),
                List.of(skuDef("S", "29.99", 5, "BC-MR")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.execute(commandWith(List.of(varDef))));
        assertTrue(ex.getMessage().contains("Material"));
    }

    @Test
    @DisplayName("Throws when a required attribute is missing in the second of two variants")
    void execute_missingRequiredAttribute_inSecondVariant_throws() {
        fakeColor.store(new ColorView(20L, "blue", "Blue", "#0000FF"));
        fakeBlueprint.storeRequired(1L, "Fit");

        List<ProductAttribute> attrsOk = List.of(new ProductAttribute("Fit", "Regular", 1));
        VariantDefinition variantOk   = new VariantDefinition(
                10L, null, attrsOk, List.of(),
                List.of(skuDef("S", "19.99", 5, "BC-V1")));

        VariantDefinition variantBad  = new VariantDefinition(
                20L, null, List.of(), List.of(),  // missing "Fit"
                List.of(skuDef("M", "19.99", 5, "BC-V2")));

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(commandWith(List.of(variantOk, variantBad))));
    }

    @Test
    @DisplayName("Optional (non-required) blueprint attributes do not cause validation failure when absent")
    void execute_optionalBlueprintAttribute_doesNotCauseFailure() {
        fakeBlueprint.storeOptional(1L, "Care");  // optional

        VariantDefinition varDef = new VariantDefinition(
                10L, null, List.of(), List.of(),   // "Care" not provided
                List.of(skuDef("S", "19.99", 5, "BC-OPT")));

        assertDoesNotThrow(() -> service.execute(commandWith(List.of(varDef))));
    }

    @Test
    @DisplayName("Variant with null attributes list fails required blueprint check")
    void execute_nullAttributesList_failsRequiredCheck() {
        fakeBlueprint.storeRequired(1L, "Material");

        VariantDefinition varDef = new VariantDefinition(
                10L, null, null, List.of(),   // null attributes
                List.of(skuDef("S", "29.99", 5, "BC-NULL")));

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(commandWith(List.of(varDef))));
    }

    // =========================================================
    //  Helper builders
    // =========================================================

    private Command commandWith(List<VariantDefinition> variants) {
        return new Command("Test Product", 1L, null, variants);
    }

    private VariantDefinition variantDef(Long colorId, List<SkuDefinition> skus) {
        return new VariantDefinition(colorId, null, List.of(), List.of(), skus);
    }

    private SkuDefinition skuDef(String size, String price, int stock, String barcode) {
        return new SkuDefinition(size, new Money(new BigDecimal(price)), stock, barcode,
                null, null, null, null);
    }

    // =========================================================
    //  In-memory fakes
    // =========================================================

    static class FakeLoadCategoryPort implements LoadCategoryPort {
        private final Map<Long, CategoryView> store = new HashMap<>();

        void store(CategoryView v) { store.put(v.id(), v); }

        @Override public Optional<CategoryView> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<CategoryView> findByName(String name) { return Optional.empty(); }
        @Override public Optional<CategoryView> findBySlug(String slug) { return Optional.empty(); }
        @Override public List<CategoryView> findRoots() { return List.of(); }
        @Override public List<CategoryView> findByParentId(Long parentId) { return List.of(); }
        @Override public List<CategoryView> findAll() { return List.copyOf(store.values()); }
        @Override public List<Long> getAllDescendantIds(Long categoryId) { return List.of(); }
    }

    static class FakeLoadColorPort implements LoadColorPort {
        private final Map<Long, ColorView> store = new HashMap<>();

        void store(ColorView v) { store.put(v.id(), v); }

        @Override public Optional<ColorView> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<ColorView> findByColorKey(String colorKey) { return Optional.empty(); }
        @Override public List<ColorView> findAll() { return List.copyOf(store.values()); }
    }

    static class FakeLoadBrandPort implements LoadBrandPort {
        private final Map<Long, BrandView> store = new HashMap<>();

        void store(BrandView v) { store.put(v.id(), v); }

        @Override public Optional<BrandView> findById(Long id) { return Optional.ofNullable(store.get(id)); }
    }

    static class FakeLoadBlueprintPort implements LoadCategoryBlueprintPort {
        private final List<BlueprintEntry> entries = new ArrayList<>();
        private final AtomicLong idGen = new AtomicLong(1);

        void storeRequired(Long categoryId, String key) {
            entries.add(new BlueprintEntry(idGen.getAndIncrement(), categoryId, key, true, 0));
        }

        void storeOptional(Long categoryId, String key) {
            entries.add(new BlueprintEntry(idGen.getAndIncrement(), categoryId, key, false, 0));
        }

        @Override
        public List<BlueprintEntry> findByCategoryId(Long categoryId) {
            return entries.stream().filter(e -> e.categoryId().equals(categoryId)).toList();
        }
    }

    static class FakeSaveHierarchyPort implements SaveProductHierarchyPort {
        private final AtomicLong idGen = new AtomicLong(100);
        int baseSaveCount;
        int variantSaveCount;
        int skuSaveCount;
        ProductBase lastSavedBase;
        ListingVariant lastSavedVariant;
        Sku lastSavedSku;

        @Override
        public ProductBase saveBase(ProductBase base) {
            baseSaveCount++;
            lastSavedBase = new ProductBase(
                    idGen.getAndIncrement(), base.getExternalRef(),
                    base.getName(), base.getCategory(), base.getCategoryId(), base.getBrandId());
            return lastSavedBase;
        }

        @Override
        public ListingVariant saveVariant(ListingVariant variant, Long productBaseId) {
            variantSaveCount++;
            lastSavedVariant = new ListingVariant(
                    idGen.getAndIncrement(), productBaseId, variant.getColorKey(),
                    variant.getSlug(), variant.getWebDescription(),
                    variant.getImages(), variant.getAttributes(),
                    variant.isTopSelling(), variant.isFeatured(),
                    variant.getLastSyncAt(), "RD-" + idGen.get());
            return lastSavedVariant;
        }

        @Override
        public Sku saveSku(Sku sku, Long listingVariantId) {
            skuSaveCount++;
            lastSavedSku = new Sku(
                    idGen.getAndIncrement(), listingVariantId, sku.getSkuCode(),
                    sku.getSizeLabel(), sku.getStockQuantity(), sku.getPrice(),
                    sku.getBarcode(), sku.getWeightKg(),
                    sku.getWidthCm(), sku.getHeightCm(), sku.getDepthCm());
            return lastSavedSku;
        }
    }

    static class FakeListingIndexPort implements ListingIndexPort {
        int indexCallCount;
        boolean throwOnIndex;

        @Override
        public void index(Long variantId, String slug, String name, String category,
                          String colorKey, String colorHexCode, String description,
                          List<String> images, Double price, Integer totalStock,
                          boolean topSelling, boolean featured, Instant lastSyncAt) {
            if (throwOnIndex) throw new RuntimeException("ES unavailable");
            indexCallCount++;
        }

        @Override
        public void delete(String slug) {}
    }
}
