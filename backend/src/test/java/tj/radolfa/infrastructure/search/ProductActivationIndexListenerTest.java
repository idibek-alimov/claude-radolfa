package tj.radolfa.infrastructure.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.event.ProductActivatedEvent;
import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.LoadBrandPort;
import tj.radolfa.application.ports.out.LoadColorPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.services.ListingVariantIndexPayload;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.domain.model.Sku;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProductActivationIndexListenerTest {

    static final Long BASE_ID     = 1L;
    static final Long VARIANT_A   = 10L;
    static final Long VARIANT_B   = 11L;

    // ── Fakes ─────────────────────────────────────────────────────────────────

    record IndexCall(Long variantId, String status) {}

    static class RecordingListingIndexPort implements ListingIndexPort {
        final List<IndexCall> calls = new ArrayList<>();
        @Override
        public void index(Long variantId, Long productBaseId, String slug, String name, String category,
                          String colorKey, String colorHexCode,
                          String description, List<String> images,
                          Double price, Integer totalStock,
                          Instant lastSyncAt,
                          String productCode, List<String> skuCodes,
                          String status,
                          Long categoryId, Long brandId, String brandName,
                          Integer discountPercentage, Double ratingAverage,
                          Instant createdAt) {
            calls.add(new IndexCall(variantId, status));
        }
        @Override public void delete(String slug) {}
    }

    static LoadProductBasePort basePort(ProductBase base) {
        return new LoadProductBasePort() {
            @Override public Optional<ProductBase> findById(Long id) {
                return BASE_ID.equals(id) ? Optional.of(base) : Optional.empty();
            }
            @Override public Optional<ProductBase> findByExternalRef(String r) { return Optional.empty(); }
            @Override public Map<Long, ProductBase> findProductsByIds(Collection<Long> ids) { return Map.of(); }
        };
    }

    static LoadListingVariantPort variantPort(List<ListingVariant> variants) {
        return new LoadListingVariantPort() {
            @Override public List<ListingVariant> findAllByProductBaseId(Long id) { return variants; }
            @Override public Optional<ListingVariant> findVariantById(Long id) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long b, String c) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findBySlug(String s) { return Optional.empty(); }
            @Override public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) { return Map.of(); }
        };
    }

    static LoadSkuPort noSkus() {
        return new LoadSkuPort() {
            @Override public Optional<Sku> findBySkuCode(String c) { return Optional.empty(); }
            @Override public Optional<Sku> findSkuById(Long id) { return Optional.empty(); }
            @Override public List<Sku> findSkusByVariantId(Long id) { return List.of(); }
            @Override public List<Sku> findAllByIds(Collection<Long> ids) { return List.of(); }
        };
    }

    static LoadColorPort noColors() {
        return new LoadColorPort() {
            @Override public Optional<ColorView> findByColorKey(String k) { return Optional.empty(); }
            @Override public List<ColorView> findAll() { return List.of(); }
            @Override public Optional<ColorView> findById(Long id) { return Optional.empty(); }
        };
    }

    static LoadBrandPort noBrands() {
        return new LoadBrandPort() {
            @Override public Optional<LoadBrandPort.BrandView> findById(Long id) { return Optional.empty(); }
        };
    }

    static ListingVariant variant(Long id, Long baseId) {
        return new ListingVariant(id, baseId, "red", "slug-" + id, null,
                List.of(), List.of(), List.of(), null, "00001", true, true,
                null, null, null, null);
    }

    static ProductBase activeBase() {
        return new ProductBase(BASE_ID, "EXT-001", "Test Product", "Clothing",
                1L, null, ProductStatus.ACTIVE, null);
    }

    ProductActivationIndexListener listener(LoadListingVariantPort variants,
                                            RecordingListingIndexPort recorder) {
        ListingVariantIndexPayload payload =
                new ListingVariantIndexPayload(noSkus(), noColors(), noBrands());
        return new ProductActivationIndexListener(
                basePort(activeBase()), variants, payload, recorder);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("One index call per variant, each carrying status=ACTIVE")
    void onProductActivated_indexesAllVariantsWithActiveStatus() {
        var variants = List.of(variant(VARIANT_A, BASE_ID), variant(VARIANT_B, BASE_ID));
        var recorder = new RecordingListingIndexPort();
        var svc = listener(variantPort(variants), recorder);

        svc.onProductActivated(new ProductActivatedEvent(BASE_ID));

        assertEquals(2, recorder.calls.size(), "one call per variant");
        assertTrue(recorder.calls.stream().allMatch(c -> "ACTIVE".equals(c.status())),
                "every indexed doc must carry status=ACTIVE");
        assertTrue(recorder.calls.stream().anyMatch(c -> VARIANT_A.equals(c.variantId())));
        assertTrue(recorder.calls.stream().anyMatch(c -> VARIANT_B.equals(c.variantId())));
    }

    @Test
    @DisplayName("No variants → no index calls, no exception")
    void onProductActivated_noVariants_noIndex() {
        var recorder = new RecordingListingIndexPort();
        var svc = listener(variantPort(List.of()), recorder);

        svc.onProductActivated(new ProductActivatedEvent(BASE_ID));

        assertTrue(recorder.calls.isEmpty());
    }

    @Test
    @DisplayName("Unknown productBaseId → silently no-ops, no exception")
    void onProductActivated_unknownBase_noOp() {
        var recorder = new RecordingListingIndexPort();
        // Override the base port to return empty for any id
        ListingVariantIndexPayload payload = new ListingVariantIndexPayload(noSkus(), noColors(), noBrands());
        LoadProductBasePort missingBase = new LoadProductBasePort() {
            @Override public Optional<ProductBase> findById(Long id) { return Optional.empty(); }
            @Override public Optional<ProductBase> findByExternalRef(String r) { return Optional.empty(); }
            @Override public Map<Long, ProductBase> findProductsByIds(Collection<Long> ids) { return Map.of(); }
        };
        var svc = new ProductActivationIndexListener(
                missingBase, variantPort(List.of(variant(VARIANT_A, BASE_ID))),
                payload, recorder);

        svc.onProductActivated(new ProductActivatedEvent(999L));

        assertTrue(recorder.calls.isEmpty());
    }
}
