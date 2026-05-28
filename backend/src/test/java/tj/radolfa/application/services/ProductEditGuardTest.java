package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadProductForSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.domain.model.Sku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProductEditGuardTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeProductBaseStore implements LoadProductBasePort, SaveProductHierarchyPort {
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

    static ProductBase withStatus(Long id, ProductStatus status) {
        return new ProductBase(id, "EXT-" + id, "Name", null, null, null, status, null);
    }

    static ProductEditGuard guard(FakeProductBaseStore store,
                                  LoadProductForSkuPort skuPort,
                                  LoadListingVariantPort variantPort) {
        return new ProductEditGuard(store, store, skuPort, variantPort);
    }

    static LoadListingVariantPort noVariantPort() {
        return new LoadListingVariantPort() {
            @Override public Optional<ListingVariant> findVariantById(Long id) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long p, String c) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findBySlug(String s) { return Optional.empty(); }
            @Override public List<ListingVariant> findAllByProductBaseId(Long id) { return List.of(); }
            @Override public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) { return Map.of(); }
        };
    }

    // ── resetIfNeeded ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("PENDING_REVIEW → DRAFT; save called once")
    void pendingReview_resetsToDraft_saveCalled() {
        FakeProductBaseStore store = new FakeProductBaseStore();
        store.put(withStatus(1L, ProductStatus.PENDING_REVIEW));

        guard(store, id -> Optional.empty(), noVariantPort()).resetIfNeeded(1L);

        assertEquals(ProductStatus.DRAFT, store.get(1L).getStatus());
        assertEquals(1, store.saved.size());
    }

    @Test
    @DisplayName("REJECTED → DRAFT; save called once")
    void rejected_resetsToDraft_saveCalled() {
        FakeProductBaseStore store = new FakeProductBaseStore();
        store.put(new ProductBase(1L, "EXT-1", "N", null, null, null, ProductStatus.REJECTED, "bad image"));

        guard(store, id -> Optional.empty(), noVariantPort()).resetIfNeeded(1L);

        assertEquals(ProductStatus.DRAFT, store.get(1L).getStatus());
        assertEquals(1, store.saved.size());
    }

    @Test
    @DisplayName("DRAFT → no save call")
    void draft_noSave() {
        FakeProductBaseStore store = new FakeProductBaseStore();
        store.put(withStatus(1L, ProductStatus.DRAFT));

        guard(store, id -> Optional.empty(), noVariantPort()).resetIfNeeded(1L);

        assertTrue(store.saved.isEmpty());
    }

    @Test
    @DisplayName("ACTIVE → no save call")
    void active_noSave() {
        FakeProductBaseStore store = new FakeProductBaseStore();
        store.put(withStatus(1L, ProductStatus.ACTIVE));

        guard(store, id -> Optional.empty(), noVariantPort()).resetIfNeeded(1L);

        assertTrue(store.saved.isEmpty());
    }

    @Test
    @DisplayName("AWAITING_STOCK → no save call")
    void awaitingStock_noSave() {
        FakeProductBaseStore store = new FakeProductBaseStore();
        store.put(withStatus(1L, ProductStatus.AWAITING_STOCK));

        guard(store, id -> Optional.empty(), noVariantPort()).resetIfNeeded(1L);

        assertTrue(store.saved.isEmpty());
    }

    // ── resetIfNeededBySkuId ──────────────────────────────────────────────────

    @Test
    @DisplayName("resetIfNeededBySkuId resolves productBaseId through port")
    void resetIfNeededBySkuId_resolvesAndResets() {
        FakeProductBaseStore store = new FakeProductBaseStore();
        store.put(withStatus(10L, ProductStatus.PENDING_REVIEW));

        LoadProductForSkuPort skuPort = skuId -> Optional.of(10L);
        guard(store, skuPort, noVariantPort()).resetIfNeededBySkuId(99L);

        assertEquals(ProductStatus.DRAFT, store.get(10L).getStatus());
    }

    @Test
    @DisplayName("resetIfNeededBySkuId: port returns empty → no action")
    void resetIfNeededBySkuId_emptyPort_noAction() {
        FakeProductBaseStore store = new FakeProductBaseStore();

        guard(store, id -> Optional.empty(), noVariantPort()).resetIfNeededBySkuId(99L);

        assertTrue(store.saved.isEmpty());
    }

    // ── resetIfNeededByVariantId ───────────────────────────────────────────────

    @Test
    @DisplayName("resetIfNeededByVariantId resolves productBaseId through variant port")
    void resetIfNeededByVariantId_resolvesAndResets() {
        FakeProductBaseStore store = new FakeProductBaseStore();
        store.put(withStatus(5L, ProductStatus.PENDING_REVIEW));

        ListingVariant variant = new ListingVariant(
                20L, 5L, "red", "slug", null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                null, null, true, true, null, null, null, null);
        LoadListingVariantPort variantPort = new LoadListingVariantPort() {
            @Override public Optional<ListingVariant> findVariantById(Long id) {
                return id.equals(20L) ? Optional.of(variant) : Optional.empty();
            }
            @Override public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long p, String c) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findBySlug(String s) { return Optional.empty(); }
            @Override public List<ListingVariant> findAllByProductBaseId(Long id) { return List.of(); }
            @Override public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) { return Map.of(); }
        };

        guard(store, id -> Optional.empty(), variantPort).resetIfNeededByVariantId(20L);

        assertEquals(ProductStatus.DRAFT, store.get(5L).getStatus());
    }
}
