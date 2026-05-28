package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.ports.out.LoadColorPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.application.readmodel.CategoryView;
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

class UpdateProductCategoryServiceTest {

    private static final Long BASE_ID     = 1L;
    private static final Long CATEGORY_ID = 50L;

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

    static ProductBase withStatus(Long id, ProductStatus status) {
        return new ProductBase(id, "EXT-" + id, "Name", null, null, null, status, null);
    }

    static LoadListingVariantPort noVariants() {
        return new LoadListingVariantPort() {
            @Override public Optional<ListingVariant> findVariantById(Long id) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long p, String c) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findBySlug(String s) { return Optional.empty(); }
            @Override public List<ListingVariant> findAllByProductBaseId(Long id) { return List.of(); }
            @Override public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) { return Map.of(); }
        };
    }

    static LoadCategoryPort categoryPort() {
        CategoryView cat = new CategoryView(CATEGORY_ID, "Shoes", "shoes", null, List.of());
        return new LoadCategoryPort() {
            @Override public Optional<CategoryView> findById(Long id) {
                return CATEGORY_ID.equals(id) ? Optional.of(cat) : Optional.empty();
            }
            @Override public Optional<CategoryView> findByName(String n) { return Optional.empty(); }
            @Override public Optional<CategoryView> findBySlug(String s) { return Optional.empty(); }
            @Override public List<CategoryView> findRoots() { return List.of(); }
            @Override public List<CategoryView> findByParentId(Long id) { return List.of(); }
            @Override public List<CategoryView> findAll() { return List.of(); }
            @Override public List<Long> getAllDescendantIds(Long id) { return List.of(); }
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

    UpdateProductCategoryService service(FakeBaseStore store) {
        ProductEditGuard guard = new ProductEditGuard(
                store, store, skuId -> Optional.empty(), noVariants());
        ListingVariantIndexPayload payload = new ListingVariantIndexPayload(noSkus(), noColors());
        return new UpdateProductCategoryService(
                store, categoryPort(), noVariants(),
                store, event -> { /* no-op event publisher */ }, guard, payload);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PENDING_REVIEW → DRAFT on category update; new category persisted")
    void pendingReview_resetsToDraft_categoryUpdated() {
        FakeBaseStore store = new FakeBaseStore();
        store.put(withStatus(BASE_ID, ProductStatus.PENDING_REVIEW));

        service(store).execute(BASE_ID, CATEGORY_ID);

        assertEquals(ProductStatus.DRAFT, store.get(BASE_ID).getStatus());
        assertEquals(CATEGORY_ID, store.get(BASE_ID).getCategoryId());
    }

    @Test
    @DisplayName("REJECTED → DRAFT on category update")
    void rejected_resetsToDraft_categoryUpdated() {
        FakeBaseStore store = new FakeBaseStore();
        store.put(new ProductBase(BASE_ID, "EXT-1", "Name", null, null, null,
                ProductStatus.REJECTED, "bad image"));

        service(store).execute(BASE_ID, CATEGORY_ID);

        assertEquals(ProductStatus.DRAFT, store.get(BASE_ID).getStatus());
        assertEquals(CATEGORY_ID, store.get(BASE_ID).getCategoryId());
    }

    @Test
    @DisplayName("ACTIVE → status unchanged; category updated")
    void active_statusUnchanged_categoryUpdated() {
        FakeBaseStore store = new FakeBaseStore();
        store.put(withStatus(BASE_ID, ProductStatus.ACTIVE));

        service(store).execute(BASE_ID, CATEGORY_ID);

        assertEquals(ProductStatus.ACTIVE, store.get(BASE_ID).getStatus());
        assertEquals(CATEGORY_ID, store.get(BASE_ID).getCategoryId());
    }

    @Test
    @DisplayName("Guard does not save for ACTIVE")
    void active_guardDoesNotSave() {
        FakeBaseStore store = new FakeBaseStore();
        store.put(withStatus(BASE_ID, ProductStatus.ACTIVE));

        service(store).execute(BASE_ID, CATEGORY_ID);

        // only the category-update saveBase, not an extra guard save
        assertEquals(1, store.saved.size());
    }

    @Test
    @DisplayName("Guard saves once + service saves once for PENDING_REVIEW")
    void pendingReview_twoSaves() {
        FakeBaseStore store = new FakeBaseStore();
        store.put(withStatus(BASE_ID, ProductStatus.PENDING_REVIEW));

        service(store).execute(BASE_ID, CATEGORY_ID);

        assertEquals(2, store.saved.size());
    }

    @Test
    @DisplayName("Unknown category → IllegalArgumentException")
    void unknownCategory_throws() {
        FakeBaseStore store = new FakeBaseStore();
        store.put(withStatus(BASE_ID, ProductStatus.ACTIVE));

        assertThrows(IllegalArgumentException.class,
                () -> service(store).execute(BASE_ID, 999L));
    }

    @Test
    @DisplayName("Unknown product → ResourceNotFoundException (guard fires first)")
    void unknownProduct_throws() {
        FakeBaseStore store = new FakeBaseStore();

        assertThrows(ResourceNotFoundException.class,
                () -> service(store).execute(999L, CATEGORY_ID));
    }
}
