package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.warehouse.LookupSkuByBarcodeUseCase;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuByBarcodePort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.application.readmodel.InboundQueueItem;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.InventoryPlacement;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.PlacementView;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.Warehouse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LookupSkuByBarcodeServiceTest {

    static final Long SKU_ID     = 1L;
    static final Long VARIANT_ID = 10L;
    static final Long BASE_ID    = 100L;
    static final Long WH_ID      = 1L;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    static Sku sku(String barcode) {
        return new Sku(SKU_ID, VARIANT_ID, "SKU-001", "M",
                5, new Money(BigDecimal.TEN), barcode);
    }

    static ListingVariant variant() {
        return new ListingVariant(VARIANT_ID, BASE_ID, "black", "widget-black",
                null, List.of(), List.of(), List.of(), null, null,
                true, true, null, null, null, null);
    }

    static ProductBase productBase(String name) {
        return new ProductBase(BASE_ID, "EXT-001", name, "Category", null, null, tj.radolfa.domain.model.ProductStatus.DRAFT, null);
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeLoadSkuByBarcodePort implements LoadSkuByBarcodePort {
        final Map<String, Sku> store;
        FakeLoadSkuByBarcodePort(Map<String, Sku> store) { this.store = store; }
        @Override public Optional<Sku> findByBarcode(String barcode) {
            return Optional.ofNullable(store.get(barcode));
        }
    }

    static class FakeLoadListingVariantPort implements LoadListingVariantPort {
        final ListingVariant variant;
        FakeLoadListingVariantPort(ListingVariant variant) { this.variant = variant; }
        @Override public Optional<ListingVariant> findVariantById(Long id) {
            return variant != null && variant.getId().equals(id) ? Optional.of(variant) : Optional.empty();
        }
        @Override public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long pid, String ck) { return Optional.empty(); }
        @Override public Optional<ListingVariant> findBySlug(String s) { return Optional.empty(); }
        @Override public List<ListingVariant> findAllByProductBaseId(Long id) { return List.of(); }
        @Override public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) { return Map.of(); }
    }

    static class FakeLoadProductBasePort implements LoadProductBasePort {
        final ProductBase base;
        FakeLoadProductBasePort(ProductBase base) { this.base = base; }
        @Override public Optional<ProductBase> findById(Long id) {
            return base != null && base.getId().equals(id) ? Optional.of(base) : Optional.empty();
        }
        @Override public Optional<ProductBase> findByExternalRef(String ref) { return Optional.empty(); }
        @Override public Map<Long, ProductBase> findProductsByIds(Collection<Long> ids) { return Map.of(); }
    }

    static class FakeLoadWarehousePort implements LoadWarehousePort {
        @Override public Warehouse findDefault() {
            return new Warehouse(WH_ID, "MAIN", "Main Warehouse", true, Instant.now());
        }
        @Override public Optional<Warehouse> findById(Long id) {
            return id.equals(WH_ID) ? Optional.of(findDefault()) : Optional.empty();
        }
    }

    static class FakePlacementPort implements InventoryPlacementPort {
        final List<PlacementView> views;
        FakePlacementPort(List<PlacementView> views) { this.views = views; }

        @Override public List<PlacementView> placementViewsForSku(Long s, Long w)        { return views; }
        @Override public Map<Long, List<PlacementView>> placementViewsForSkus(Collection<Long> ids, Long w) {
            return ids.isEmpty() ? Map.of() : Map.of(ids.iterator().next(), views);
        }
        @Override public void addToInbound(Long s, Long w, int q)               {}
        @Override public boolean decrementForSale(Long s, Long w, int q)        { return true; }
        @Override public void putaway(Long s, Long w, Long b, int q)            {}
        @Override public void relocate(Long s, Long w, Long f, Long t, int q)   {}
        @Override public void adjustInbound(Long s, Long w, int d)              {}
        @Override public int totalForSku(Long s, Long w)                        { return 0; }
        @Override public List<InventoryPlacement> placementsForSku(Long s, Long w) { return List.of(); }
        @Override public PageResult<InboundQueueItem> findInboundQueue(int p, int sz, String q) {
            return new PageResult<>(List.of(), 0, p, sz, true);
        }
        @Override public boolean hasPlacementsInBin(Long binId)                 { return false; }
    }

    static final FakePlacementPort NO_PLACEMENTS = new FakePlacementPort(List.of());

    static LookupSkuByBarcodeService service(LoadSkuByBarcodePort barcodePort,
                                              LoadListingVariantPort variantPort,
                                              LoadProductBasePort basePort,
                                              InventoryPlacementPort placementPort) {
        return new LookupSkuByBarcodeService(barcodePort, variantPort, basePort,
                placementPort, new FakeLoadWarehousePort());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid barcode → correct SKU + productName resolved from variant chain")
    void validBarcode_returnsSkuAndProductName() {
        var port = new FakeLoadSkuByBarcodePort(Map.of("BC-001", sku("BC-001")));
        LookupSkuByBarcodeUseCase.Result result =
                service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(productBase("Widget")), NO_PLACEMENTS).execute("BC-001");

        assertEquals(SKU_ID, result.sku().getId());
        assertEquals("BC-001", result.sku().getBarcode());
        assertEquals("Widget", result.productName());
        assertTrue(result.placements().isEmpty());
    }

    @Test
    @DisplayName("Unknown barcode → ResourceNotFoundException")
    void unknownBarcode_throws() {
        var port = new FakeLoadSkuByBarcodePort(Map.of());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(productBase("Widget")), NO_PLACEMENTS).execute("NOTEXIST"));

        assertTrue(ex.getMessage().contains("NOTEXIST"));
    }

    @Test
    @DisplayName("Variant lookup fails → productName falls back to skuCode")
    void variantMissing_fallsBackToSkuCode() {
        var port = new FakeLoadSkuByBarcodePort(Map.of("BC-001", sku("BC-001")));
        LookupSkuByBarcodeUseCase.Result result =
                service(port, new FakeLoadListingVariantPort(null),
                        new FakeLoadProductBasePort(productBase("Widget")), NO_PLACEMENTS).execute("BC-001");

        assertEquals("SKU-001", result.productName());
    }

    @Test
    @DisplayName("Product base lookup fails → productName falls back to skuCode")
    void productBaseMissing_fallsBackToSkuCode() {
        var port = new FakeLoadSkuByBarcodePort(Map.of("BC-001", sku("BC-001")));
        LookupSkuByBarcodeUseCase.Result result =
                service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(null), NO_PLACEMENTS).execute("BC-001");

        assertEquals("SKU-001", result.productName());
    }

    @Test
    @DisplayName("Blank barcode → ResourceNotFoundException")
    void blankBarcode_throws() {
        var port = new FakeLoadSkuByBarcodePort(Map.of());
        assertThrows(ResourceNotFoundException.class,
                () -> service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(productBase("Widget")), NO_PLACEMENTS).execute(""));
    }

    @Test
    @DisplayName("Multi-bin SKU → placements list contains labeled bin entries")
    void multiBinSku_returnsLabeledPlacements() {
        var views = List.of(
                new PlacementView(1L, "A-1-1", 30),
                new PlacementView(2L, "B-2-3", 20),
                new PlacementView(null, null, 10));
        var port = new FakeLoadSkuByBarcodePort(Map.of("BC-002", sku("BC-002")));
        LookupSkuByBarcodeUseCase.Result result =
                service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(productBase("Widget")),
                        new FakePlacementPort(views)).execute("BC-002");

        assertEquals(3, result.placements().size());
        assertEquals("A-1-1", result.placements().get(0).binLabel());
        assertEquals(30, result.placements().get(0).quantity());
        assertEquals("B-2-3", result.placements().get(1).binLabel());
        assertNull(result.placements().get(2).binLabel(), "inbound pool entry has null binLabel");
        assertEquals(10, result.placements().get(2).quantity());
    }

    @Test
    @DisplayName("Inbound-only SKU → one placement with null binLabel")
    void inboundOnlySku_returnsInboundPlacement() {
        var views = List.of(new PlacementView(null, null, 50));
        var port = new FakeLoadSkuByBarcodePort(Map.of("BC-003", sku("BC-003")));
        LookupSkuByBarcodeUseCase.Result result =
                service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(productBase("Widget")),
                        new FakePlacementPort(views)).execute("BC-003");

        assertEquals(1, result.placements().size());
        assertNull(result.placements().get(0).binLabel());
        assertEquals(50, result.placements().get(0).quantity());
    }
}
