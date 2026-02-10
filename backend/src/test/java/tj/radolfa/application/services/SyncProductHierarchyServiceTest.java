package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand.VariantCommand;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand.SkuCommand;
import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SyncProductHierarchyService}.
 *
 * <p>Uses in-memory fakes (not Mockito) to verify:
 * <ul>
 *   <li>Idempotent upsert across all 3 tiers</li>
 *   <li>Enrichment fields are never overwritten on existing variants</li>
 *   <li>Price/stock on SKUs are always overwritten</li>
 * </ul>
 */
class SyncProductHierarchyServiceTest {

    private SyncProductHierarchyService service;
    private FakeLoadProductBasePort     fakeLoadBase;
    private FakeLoadListingVariantPort  fakeLoadVariant;
    private FakeLoadSkuPort             fakeLoadSku;
    private FakeSavePort                fakeSave;
    private FakeListingIndexPort        fakeIndexPort;

    @BeforeEach
    void setUp() {
        fakeLoadBase    = new FakeLoadProductBasePort();
        fakeLoadVariant = new FakeLoadListingVariantPort();
        fakeLoadSku     = new FakeLoadSkuPort();
        fakeSave        = new FakeSavePort();
        fakeIndexPort   = new FakeListingIndexPort();

        service = new SyncProductHierarchyService(
                fakeLoadBase, fakeLoadVariant, fakeLoadSku, fakeSave, fakeIndexPort
        );
    }

    @Test
    @DisplayName("Creates new ProductBase, ListingVariant, and Sku when none exist")
    void createsFullHierarchyFromScratch() {
        HierarchySyncCommand command = buildCommand(
                "TPL-001", "T-Shirt", "red",
                "TPL-001-RED-S", "S", 50,
                new BigDecimal("29.99"), new BigDecimal("24.99"), null
        );

        ProductBase result = service.execute(command);

        assertNotNull(result.getId());
        assertEquals("TPL-001", result.getErpTemplateCode());
        assertEquals("T-Shirt", result.getName());
        assertEquals(1, fakeSave.baseSaveCount);
        assertEquals(1, fakeSave.variantSaveCount);
        assertEquals(1, fakeSave.skuSaveCount);
    }

    @Test
    @DisplayName("Upsert: updates existing ProductBase name without creating duplicates")
    void updatesExistingProductBase() {
        // Pre-populate with existing base
        fakeLoadBase.existing = new ProductBase(1L, "TPL-001", "Old Name");

        HierarchySyncCommand command = buildCommand(
                "TPL-001", "New Name", "red",
                "TPL-001-RED-S", "S", 50,
                new BigDecimal("29.99"), new BigDecimal("24.99"), null
        );

        ProductBase result = service.execute(command);

        assertEquals("New Name", result.getName());
        assertEquals(1, fakeSave.baseSaveCount, "Should save once, not create a new one");
    }

    @Test
    @DisplayName("Preserves enrichment fields on existing ListingVariant")
    void preservesEnrichmentOnExistingVariant() {
        fakeLoadBase.existing = new ProductBase(1L, "TPL-001", "T-Shirt");

        // Existing variant WITH enrichment data
        fakeLoadVariant.existing = new ListingVariant(
                10L, 1L, "red", "tpl-001-red",
                "Beautiful red cotton t-shirt",       // webDescription — enrichment
                List.of("https://s3/img1.jpg"),       // images — enrichment
                Instant.now().minusSeconds(3600)
        );

        HierarchySyncCommand command = buildCommand(
                "TPL-001", "T-Shirt", "red",
                "TPL-001-RED-S", "S", 50,
                new BigDecimal("29.99"), new BigDecimal("24.99"), null
        );

        service.execute(command);

        // The variant was saved with updated sync time but enrichment must survive
        ListingVariant saved = fakeSave.lastSavedVariant;
        assertNotNull(saved);
        assertEquals(10L, saved.getId());
        // Enrichment preserved from original (not overwritten to null)
        assertEquals("Beautiful red cotton t-shirt", saved.getWebDescription());
        assertEquals(List.of("https://s3/img1.jpg"), saved.getImages());
    }

    @Test
    @DisplayName("Always overwrites price and stock on existing SKU")
    void alwaysOverwritesPriceAndStockOnExistingSku() {
        fakeLoadBase.existing = new ProductBase(1L, "TPL-001", "T-Shirt");
        fakeLoadVariant.existing = new ListingVariant(
                10L, 1L, "red", "tpl-001-red", null,
                Collections.emptyList(), null
        );

        // Existing SKU with old price/stock
        fakeLoadSku.existing = new Sku(
                100L, 10L, "TPL-001-RED-S", "S",
                5,                                      // old stock
                new Money(new BigDecimal("19.99")),     // old price
                new Money(new BigDecimal("14.99")),     // old salePrice
                null
        );

        HierarchySyncCommand command = buildCommand(
                "TPL-001", "T-Shirt", "red",
                "TPL-001-RED-S", "S", 50,               // new stock = 50
                new BigDecimal("29.99"),                 // new price
                new BigDecimal("24.99"),                 // new salePrice
                null
        );

        service.execute(command);

        Sku saved = fakeSave.lastSavedSku;
        assertNotNull(saved);
        assertEquals(100L, saved.getId());
        assertEquals(50, saved.getStockQuantity());
        assertEquals(new BigDecimal("29.99"), saved.getPrice().amount());
        assertEquals(new BigDecimal("24.99"), saved.getSalePrice().amount());
    }

    @Test
    @DisplayName("Handles multiple variants and SKUs in one command")
    void handlesMultipleVariantsAndSkus() {
        HierarchySyncCommand command = new HierarchySyncCommand(
                "TPL-002", "Jacket",
                List.of(
                        new VariantCommand("blue", List.of(
                                new SkuCommand("J-BLUE-S", "S", 10,
                                        Money.of(new BigDecimal("89.99")),
                                        Money.of(new BigDecimal("79.99")), null),
                                new SkuCommand("J-BLUE-M", "M", 15,
                                        Money.of(new BigDecimal("89.99")),
                                        Money.of(new BigDecimal("79.99")), null)
                        )),
                        new VariantCommand("black", List.of(
                                new SkuCommand("J-BLACK-L", "L", 8,
                                        Money.of(new BigDecimal("89.99")),
                                        Money.of(new BigDecimal("89.99")), null)
                        ))
                )
        );

        ProductBase result = service.execute(command);

        assertNotNull(result);
        assertEquals(1, fakeSave.baseSaveCount);
        assertEquals(2, fakeSave.variantSaveCount);
        assertEquals(3, fakeSave.skuSaveCount);
    }

    // ---- Helper ----

    private HierarchySyncCommand buildCommand(
            String templateCode, String templateName,
            String colorKey, String erpItemCode, String sizeLabel,
            int stock, BigDecimal listPrice, BigDecimal effectivePrice,
            Instant saleEndsAt) {

        return new HierarchySyncCommand(
                templateCode, templateName,
                List.of(new VariantCommand(colorKey,
                        List.of(new SkuCommand(
                                erpItemCode, sizeLabel, stock,
                                Money.of(listPrice), Money.of(effectivePrice), saleEndsAt
                        ))
                ))
        );
    }

    // ==== In-Memory Fakes ====

    static class FakeLoadProductBasePort implements LoadProductBasePort {
        ProductBase existing;

        @Override
        public Optional<ProductBase> findByErpTemplateCode(String code) {
            return existing != null && existing.getErpTemplateCode().equals(code)
                    ? Optional.of(existing) : Optional.empty();
        }
    }

    static class FakeLoadListingVariantPort implements LoadListingVariantPort {
        ListingVariant existing;

        @Override
        public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long baseId, String colorKey) {
            return existing != null
                    && existing.getProductBaseId().equals(baseId)
                    && existing.getColorKey().equals(colorKey)
                    ? Optional.of(existing) : Optional.empty();
        }
    }

    static class FakeLoadSkuPort implements LoadSkuPort {
        Sku existing;

        @Override
        public Optional<Sku> findByErpItemCode(String code) {
            return existing != null && existing.getErpItemCode().equals(code)
                    ? Optional.of(existing) : Optional.empty();
        }
    }

    static class FakeListingIndexPort implements ListingIndexPort {
        AtomicInteger indexCount = new AtomicInteger();

        @Override
        public void index(Long variantId, String slug, String name, String colorKey,
                          String description, List<String> images,
                          Double priceStart, Double priceEnd, Integer totalStock,
                          Instant lastSyncAt) {
            indexCount.incrementAndGet();
        }

        @Override
        public void delete(String slug) {}
    }

    static class FakeSavePort implements SaveProductHierarchyPort {
        private final AtomicLong idGen = new AtomicLong(1);
        int baseSaveCount;
        int variantSaveCount;
        int skuSaveCount;
        ListingVariant lastSavedVariant;
        Sku lastSavedSku;

        @Override
        public ProductBase saveBase(ProductBase base) {
            baseSaveCount++;
            if (base.getId() != null) return base;
            return new ProductBase(idGen.getAndIncrement(), base.getErpTemplateCode(), base.getName());
        }

        @Override
        public ListingVariant saveVariant(ListingVariant variant, Long productBaseId) {
            variantSaveCount++;
            lastSavedVariant = variant;
            if (variant.getId() != null) return variant;
            return new ListingVariant(
                    idGen.getAndIncrement(), productBaseId,
                    variant.getColorKey(), variant.getSlug(),
                    variant.getWebDescription(), variant.getImages(),
                    variant.getLastSyncAt()
            );
        }

        @Override
        public Sku saveSku(Sku sku, Long variantId) {
            skuSaveCount++;
            lastSavedSku = sku;
            if (sku.getId() != null) return sku;
            return new Sku(
                    idGen.getAndIncrement(), variantId,
                    sku.getErpItemCode(), sku.getSizeLabel(),
                    sku.getStockQuantity(), sku.getPrice(),
                    sku.getSalePrice(), sku.getSaleEndsAt()
            );
        }
    }
}
