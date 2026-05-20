package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.warehouse.LookupSkuByBarcodeUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuByBarcodePort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;

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

    static LookupSkuByBarcodeService service(LoadSkuByBarcodePort barcodePort,
                                              LoadListingVariantPort variantPort,
                                              LoadProductBasePort basePort) {
        return new LookupSkuByBarcodeService(barcodePort, variantPort, basePort);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid barcode → correct SKU + productName resolved from variant chain")
    void validBarcode_returnsSkuAndProductName() {
        var port = new FakeLoadSkuByBarcodePort(Map.of("BC-001", sku("BC-001")));
        LookupSkuByBarcodeUseCase.Result result =
                service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(productBase("Widget"))).execute("BC-001");

        assertEquals(SKU_ID, result.sku().getId());
        assertEquals("BC-001", result.sku().getBarcode());
        assertEquals("Widget", result.productName());
    }

    @Test
    @DisplayName("Unknown barcode → ResourceNotFoundException")
    void unknownBarcode_throws() {
        var port = new FakeLoadSkuByBarcodePort(Map.of());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(productBase("Widget"))).execute("NOTEXIST"));

        assertTrue(ex.getMessage().contains("NOTEXIST"));
    }

    @Test
    @DisplayName("Variant lookup fails → productName falls back to skuCode")
    void variantMissing_fallsBackToSkuCode() {
        var port = new FakeLoadSkuByBarcodePort(Map.of("BC-001", sku("BC-001")));
        LookupSkuByBarcodeUseCase.Result result =
                service(port, new FakeLoadListingVariantPort(null),
                        new FakeLoadProductBasePort(productBase("Widget"))).execute("BC-001");

        assertEquals("SKU-001", result.productName());
    }

    @Test
    @DisplayName("Product base lookup fails → productName falls back to skuCode")
    void productBaseMissing_fallsBackToSkuCode() {
        var port = new FakeLoadSkuByBarcodePort(Map.of("BC-001", sku("BC-001")));
        LookupSkuByBarcodeUseCase.Result result =
                service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(null)).execute("BC-001");

        assertEquals("SKU-001", result.productName());
    }

    @Test
    @DisplayName("Blank barcode → ResourceNotFoundException")
    void blankBarcode_throws() {
        var port = new FakeLoadSkuByBarcodePort(Map.of());
        assertThrows(ResourceNotFoundException.class,
                () -> service(port, new FakeLoadListingVariantPort(variant()),
                        new FakeLoadProductBasePort(productBase("Widget"))).execute(""));
    }
}
