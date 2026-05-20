package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.warehouse.LookupSkuByBarcodeUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuByBarcodePort;
import tj.radolfa.application.ports.out.LoadWarehouseLocationPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.WarehouseBin;
import tj.radolfa.domain.model.WarehouseShelf;
import tj.radolfa.domain.model.WarehouseZone;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LookupSkuByBarcodeServiceTest {

    static final Long SKU_ID      = 1L;
    static final Long VARIANT_ID  = 10L;
    static final Long BASE_ID     = 100L;
    static final Long ZONE_ID     = 1000L;
    static final Long SHELF_ID    = 2000L;
    static final Long BIN_ID      = 3000L;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    static Sku sku(String barcode) {
        return new Sku(SKU_ID, VARIANT_ID, "SKU-001", "M",
                5, new Money(BigDecimal.TEN), barcode);
    }

    static Sku skuWithBin(String barcode) {
        return new Sku(SKU_ID, VARIANT_ID, "SKU-001", "M",
                5, new Money(BigDecimal.TEN), barcode,
                null, null, null, null, BIN_ID);
    }

    static ListingVariant variant() {
        return new ListingVariant(VARIANT_ID, BASE_ID, "black", "widget-black",
                null, List.of(), List.of(), List.of(), null, null,
                true, true, null, null, null, null);
    }

    static ProductBase productBase(String name) {
        return new ProductBase(BASE_ID, "EXT-001", name, "Category", null, null);
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

    static class FakeLoadWarehouseLocationPort implements LoadWarehouseLocationPort {
        final WarehouseBin   bin;
        final WarehouseShelf shelf;
        final WarehouseZone  zone;

        FakeLoadWarehouseLocationPort(WarehouseBin bin, WarehouseShelf shelf, WarehouseZone zone) {
            this.bin   = bin;
            this.shelf = shelf;
            this.zone  = zone;
        }

        @Override public List<WarehouseZone>      findAllZones()               { return List.of(); }
        @Override public Optional<WarehouseZone>  findZoneById(Long id)        { return zone != null && zone.id().equals(id) ? Optional.of(zone) : Optional.empty(); }
        @Override public List<WarehouseShelf>     findShelvesByZoneId(Long id) { return List.of(); }
        @Override public Optional<WarehouseShelf> findShelfById(Long id)       { return shelf != null && shelf.id().equals(id) ? Optional.of(shelf) : Optional.empty(); }
        @Override public List<WarehouseBin>       findBinsByShelfId(Long id)   { return List.of(); }
        @Override public Optional<WarehouseBin>   findBinById(Long id)         { return bin != null && bin.id().equals(id) ? Optional.of(bin) : Optional.empty(); }
    }

    static final FakeLoadWarehouseLocationPort NO_LOCATION =
            new FakeLoadWarehouseLocationPort(null, null, null);

    static LookupSkuByBarcodeService service(LoadSkuByBarcodePort barcodePort,
                                              LoadListingVariantPort variantPort,
                                              LoadProductBasePort basePort,
                                              LoadWarehouseLocationPort locationPort) {
        return new LookupSkuByBarcodeService(barcodePort, variantPort, basePort, locationPort);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid barcode → correct SKU + productName resolved from variant chain")
    void validBarcode_returnsSkuAndProductName() {
        var port = new FakeLoadSkuByBarcodePort(Map.of("BC-001", sku("BC-001")));
        LookupSkuByBarcodeUseCase.Result result =
                service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(productBase("Widget")), NO_LOCATION).execute("BC-001");

        assertEquals(SKU_ID, result.sku().getId());
        assertEquals("BC-001", result.sku().getBarcode());
        assertEquals("Widget", result.productName());
        assertNull(result.binLocation());
    }

    @Test
    @DisplayName("Unknown barcode → ResourceNotFoundException")
    void unknownBarcode_throws() {
        var port = new FakeLoadSkuByBarcodePort(Map.of());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(productBase("Widget")), NO_LOCATION).execute("NOTEXIST"));

        assertTrue(ex.getMessage().contains("NOTEXIST"));
    }

    @Test
    @DisplayName("Variant lookup fails → productName falls back to skuCode")
    void variantMissing_fallsBackToSkuCode() {
        var port = new FakeLoadSkuByBarcodePort(Map.of("BC-001", sku("BC-001")));
        LookupSkuByBarcodeUseCase.Result result =
                service(port, new FakeLoadListingVariantPort(null),
                        new FakeLoadProductBasePort(productBase("Widget")), NO_LOCATION).execute("BC-001");

        assertEquals("SKU-001", result.productName());
    }

    @Test
    @DisplayName("Product base lookup fails → productName falls back to skuCode")
    void productBaseMissing_fallsBackToSkuCode() {
        var port = new FakeLoadSkuByBarcodePort(Map.of("BC-001", sku("BC-001")));
        LookupSkuByBarcodeUseCase.Result result =
                service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(null), NO_LOCATION).execute("BC-001");

        assertEquals("SKU-001", result.productName());
    }

    @Test
    @DisplayName("Blank barcode → ResourceNotFoundException")
    void blankBarcode_throws() {
        var port = new FakeLoadSkuByBarcodePort(Map.of());
        assertThrows(ResourceNotFoundException.class,
                () -> service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(productBase("Widget")), NO_LOCATION).execute(""));
    }

    @Test
    @DisplayName("SKU has binId, full chain resolves → binLocation formatted correctly")
    void binAssigned_resolvesBinLocation() {
        var zone  = new WarehouseZone(ZONE_ID,  "A",  "Zone A");
        var shelf = new WarehouseShelf(SHELF_ID, ZONE_ID, "3", "Row 3");
        var bin   = new WarehouseBin(BIN_ID,   SHELF_ID, "7");
        var locationPort = new FakeLoadWarehouseLocationPort(bin, shelf, zone);

        var port = new FakeLoadSkuByBarcodePort(Map.of("BC-002", skuWithBin("BC-002")));
        LookupSkuByBarcodeUseCase.Result result =
                service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(productBase("Widget")), locationPort).execute("BC-002");

        assertEquals("A / 3 / 7", result.binLocation());
    }

    @Test
    @DisplayName("SKU has binId but bin lookup returns empty → binLocation is null, no crash")
    void binAssigned_staleBinId_returnsNullBinLocation() {
        var port = new FakeLoadSkuByBarcodePort(Map.of("BC-002", skuWithBin("BC-002")));
        LookupSkuByBarcodeUseCase.Result result =
                service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(productBase("Widget")), NO_LOCATION).execute("BC-002");

        assertNull(result.binLocation());
    }
}
