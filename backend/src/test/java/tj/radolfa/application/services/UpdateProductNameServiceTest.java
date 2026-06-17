package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.domain.model.Sku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UpdateProductNameServiceTest {

    // ── Shared fakes ──────────────────────────────────────────────────────────

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

    static ProductBase withStatus(Long id, ProductStatus status) {
        return new ProductBase(id, "EXT-" + id, "OldName", null, null, null, status, null);
    }

    static UpdateProductNameService service(FakeBaseStore store) {
        LoadListingVariantPort noVariants = new LoadListingVariantPort() {
            @Override public Optional<ListingVariant> findVariantById(Long id) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long p, String c) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findBySlug(String s) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findByProductCode(String code) { return Optional.empty(); }
            @Override public List<ListingVariant> findAllByProductBaseId(Long id) { return List.of(); }
            @Override public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) { return Map.of(); }
        };
        ProductEditGuard guard = new ProductEditGuard(store, store, skuId -> Optional.empty(), noVariants);
        return new UpdateProductNameService(store, store, guard);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PENDING_REVIEW → DRAFT on name update; new name persisted")
    void pendingReview_resetsToDraft_nameUpdated() {
        FakeBaseStore store = new FakeBaseStore();
        store.put(withStatus(1L, ProductStatus.PENDING_REVIEW));

        service(store).execute(1L, "NewName");

        assertEquals(ProductStatus.DRAFT, store.get(1L).getStatus());
        assertEquals("NewName", store.get(1L).getName());
    }

    @Test
    @DisplayName("REJECTED → DRAFT on name update; new name persisted")
    void rejected_resetsToDraft_nameUpdated() {
        FakeBaseStore store = new FakeBaseStore();
        store.put(new ProductBase(1L, "EXT-1", "OldName", null, null, null,
                ProductStatus.REJECTED, "bad image"));

        service(store).execute(1L, "NewName");

        assertEquals(ProductStatus.DRAFT, store.get(1L).getStatus());
        assertEquals("NewName", store.get(1L).getName());
    }

    @Test
    @DisplayName("ACTIVE → status unchanged; name still updated")
    void active_statusUnchanged_nameUpdated() {
        FakeBaseStore store = new FakeBaseStore();
        store.put(withStatus(1L, ProductStatus.ACTIVE));

        service(store).execute(1L, "NewName");

        assertEquals(ProductStatus.ACTIVE, store.get(1L).getStatus());
        assertEquals("NewName", store.get(1L).getName());
    }

    @Test
    @DisplayName("DRAFT → status unchanged; name updated")
    void draft_statusUnchanged_nameUpdated() {
        FakeBaseStore store = new FakeBaseStore();
        store.put(withStatus(1L, ProductStatus.DRAFT));

        service(store).execute(1L, "NewName");

        assertEquals(ProductStatus.DRAFT, store.get(1L).getStatus());
        assertEquals("NewName", store.get(1L).getName());
    }

    @Test
    @DisplayName("Guard does not save for ACTIVE (no extra save before the name save)")
    void active_guardDoesNotSave() {
        FakeBaseStore store = new FakeBaseStore();
        store.put(withStatus(1L, ProductStatus.ACTIVE));

        service(store).execute(1L, "NewName");

        // exactly 1 save: the name update itself; guard adds no extra save
        assertEquals(1, store.saved.size());
    }

    @Test
    @DisplayName("Guard saves once + service saves once for PENDING_REVIEW")
    void pendingReview_twoSaves() {
        FakeBaseStore store = new FakeBaseStore();
        store.put(withStatus(1L, ProductStatus.PENDING_REVIEW));

        service(store).execute(1L, "NewName");

        assertEquals(2, store.saved.size());
    }

    @Test
    @DisplayName("Unknown product → ResourceNotFoundException (guard fires first)")
    void unknownProduct_throws() {
        FakeBaseStore store = new FakeBaseStore();

        assertThrows(ResourceNotFoundException.class, () -> service(store).execute(999L, "X"));
    }
}
