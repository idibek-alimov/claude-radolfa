package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.domain.model.Sku;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UpdateSkuSizeLabelServiceTest {

    private static final Long BASE_ID    = 1L;
    private static final Long VARIANT_ID = 10L;
    private static final Long SKU_ID     = 100L;

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeBaseStore implements LoadProductBasePort, SaveProductHierarchyPort {
        final Map<Long, ProductBase> store = new HashMap<>();
        final List<ProductBase> saved = new ArrayList<>();

        void put(ProductBase pb) { store.put(pb.getId(), pb); }
        ProductBase get(Long id) { return store.get(id); }

        @Override public Optional<ProductBase> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<ProductBase> findByExternalRef(String r) { return Optional.empty(); }
        @Override public Map<Long, ProductBase> findProductsByIds(Collection<Long> ids) { return Map.of(); }

        @Override public ProductBase saveBase(ProductBase base) { saved.add(base); store.put(base.getId(), base); return base; }
        @Override public ListingVariant saveVariant(ListingVariant v, Long id) { return v; }
        @Override public Sku saveSku(Sku s, Long id) { return s; }
    }

    static class FakeSkuStore implements LoadSkuPort {
        final Map<Long, Sku> store = new HashMap<>();

        void put(Sku s) { store.put(s.getId(), s); }
        Sku get(Long id) { return store.get(id); }

        @Override public Optional<Sku> findBySkuCode(String c) { return Optional.empty(); }
        @Override public Optional<Sku> findSkuById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Sku> findSkusByVariantId(Long variantId) { return List.of(); }
        @Override public List<Sku> findAllByIds(Collection<Long> ids) { return List.of(); }
    }

    static ProductBase withStatus(Long id, ProductStatus status) {
        return new ProductBase(id, "EXT-" + id, "Name", null, null, null, status, null);
    }

    static Sku sku(Long id, Long variantId) {
        return new Sku(id, variantId, "SKU-001", "M", 5, new Money(new BigDecimal("29.99")));
    }

    static LoadListingVariantPort noVariants() {
        return new LoadListingVariantPort() {
            @Override public Optional<ListingVariant> findVariantById(Long id) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long p, String c) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findBySlug(String s) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findByProductCode(String code) { return Optional.empty(); }
            @Override public List<ListingVariant> findAllByProductBaseId(Long id) { return List.of(); }
            @Override public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) { return Map.of(); }
        };
    }

    UpdateSkuSizeLabelService service(FakeBaseStore baseStore, FakeSkuStore skuStore) {
        ProductEditGuard guard = new ProductEditGuard(
                baseStore, baseStore,
                skuId -> SKU_ID.equals(skuId) ? Optional.of(BASE_ID) : Optional.empty(),
                noVariants());
        return new UpdateSkuSizeLabelService(skuStore, baseStore, guard);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PENDING_REVIEW → DRAFT on size label update; new label persisted")
    void pendingReview_resetsToDraft_labelUpdated() {
        FakeBaseStore baseStore = new FakeBaseStore();
        baseStore.put(withStatus(BASE_ID, ProductStatus.PENDING_REVIEW));
        FakeSkuStore skuStore = new FakeSkuStore();
        skuStore.put(sku(SKU_ID, VARIANT_ID));

        service(baseStore, skuStore).execute(SKU_ID, "XL");

        assertEquals(ProductStatus.DRAFT, baseStore.get(BASE_ID).getStatus());
        assertEquals("XL", skuStore.get(SKU_ID).getSizeLabel());
    }

    @Test
    @DisplayName("ACTIVE → status unchanged; size label updated")
    void active_statusUnchanged_labelUpdated() {
        FakeBaseStore baseStore = new FakeBaseStore();
        baseStore.put(withStatus(BASE_ID, ProductStatus.ACTIVE));
        FakeSkuStore skuStore = new FakeSkuStore();
        skuStore.put(sku(SKU_ID, VARIANT_ID));

        service(baseStore, skuStore).execute(SKU_ID, "XL");

        assertEquals(ProductStatus.ACTIVE, baseStore.get(BASE_ID).getStatus());
        assertEquals("XL", skuStore.get(SKU_ID).getSizeLabel());
    }

    @Test
    @DisplayName("Guard does not save for ACTIVE")
    void active_guardDoesNotSave() {
        FakeBaseStore baseStore = new FakeBaseStore();
        baseStore.put(withStatus(BASE_ID, ProductStatus.ACTIVE));
        FakeSkuStore skuStore = new FakeSkuStore();
        skuStore.put(sku(SKU_ID, VARIANT_ID));

        service(baseStore, skuStore).execute(SKU_ID, "XL");

        assertTrue(baseStore.saved.isEmpty());
    }

    @Test
    @DisplayName("Guard saves once for PENDING_REVIEW")
    void pendingReview_guardSavesOnce() {
        FakeBaseStore baseStore = new FakeBaseStore();
        baseStore.put(withStatus(BASE_ID, ProductStatus.PENDING_REVIEW));
        FakeSkuStore skuStore = new FakeSkuStore();
        skuStore.put(sku(SKU_ID, VARIANT_ID));

        service(baseStore, skuStore).execute(SKU_ID, "XL");

        assertEquals(1, baseStore.saved.size());
    }

    @Test
    @DisplayName("Unknown SKU → IllegalArgumentException")
    void unknownSku_throws() {
        FakeBaseStore baseStore = new FakeBaseStore();
        baseStore.put(withStatus(BASE_ID, ProductStatus.ACTIVE));
        FakeSkuStore skuStore = new FakeSkuStore();

        assertThrows(IllegalArgumentException.class,
                () -> service(baseStore, skuStore).execute(SKU_ID, "XL"));
    }
}
