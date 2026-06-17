package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.product.SkuEditActor;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuOwnerPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.FieldLockException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.UserRole;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UpdateProductPriceServiceTest {

    private static final Long BASE_ID    = 1L;
    private static final Long VARIANT_ID = 10L;
    private static final Long SKU_ID     = 100L;
    private static final Long SELLER_A   = 50L;
    private static final Long SELLER_B   = 51L;

    private static final SkuEditActor ADMIN_ACTOR =
            new SkuEditActor(UserRole.ADMIN, 1L, null);

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

    /**
     * Fake owner port that returns the given sellerId for SKU_ID, empty otherwise.
     * sellerId=null means the SKU exists but is Radolfa-owned.
     */
    static class FakeSkuOwnerPort implements LoadSkuOwnerPort {
        private final Long ownedBySellerId; // null = Radolfa-owned

        FakeSkuOwnerPort(Long ownedBySellerId) {
            this.ownedBySellerId = ownedBySellerId;
        }

        @Override
        public Optional<SkuOwner> findBySkuId(Long skuId) {
            if (!SKU_ID.equals(skuId)) return Optional.empty();
            return Optional.of(new SkuOwner(skuId, ownedBySellerId));
        }
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

    UpdateProductPriceService service(FakeBaseStore baseStore,
                                      FakeSkuStore skuStore,
                                      LoadSkuOwnerPort ownerPort) {
        ProductEditGuard guard = new ProductEditGuard(
                baseStore, baseStore,
                skuId -> SKU_ID.equals(skuId) ? Optional.of(BASE_ID) : Optional.empty(),
                noVariants());
        return new UpdateProductPriceService(skuStore, ownerPort, baseStore, guard);
    }

    UpdateProductPriceService service(FakeBaseStore baseStore, FakeSkuStore skuStore) {
        // Default: Radolfa-owned (admin short-circuits guard for all tests)
        return service(baseStore, skuStore, new FakeSkuOwnerPort(null));
    }

    // ── Existing tests (ADMIN actor) ──────────────────────────────────────────

    @Test
    @DisplayName("PENDING_REVIEW → DRAFT on price update; new price persisted")
    void pendingReview_resetsToDraft_priceUpdated() {
        FakeBaseStore baseStore = new FakeBaseStore();
        baseStore.put(withStatus(BASE_ID, ProductStatus.PENDING_REVIEW));
        FakeSkuStore skuStore = new FakeSkuStore();
        skuStore.put(sku(SKU_ID, VARIANT_ID));

        service(baseStore, skuStore).execute(SKU_ID, new Money(new BigDecimal("49.99")), ADMIN_ACTOR);

        assertEquals(ProductStatus.DRAFT, baseStore.get(BASE_ID).getStatus());
        assertEquals(new BigDecimal("49.99"), skuStore.get(SKU_ID).getPrice().amount());
    }

    @Test
    @DisplayName("ACTIVE → status unchanged; price updated")
    void active_statusUnchanged_priceUpdated() {
        FakeBaseStore baseStore = new FakeBaseStore();
        baseStore.put(withStatus(BASE_ID, ProductStatus.ACTIVE));
        FakeSkuStore skuStore = new FakeSkuStore();
        skuStore.put(sku(SKU_ID, VARIANT_ID));

        service(baseStore, skuStore).execute(SKU_ID, new Money(new BigDecimal("99.00")), ADMIN_ACTOR);

        assertEquals(ProductStatus.ACTIVE, baseStore.get(BASE_ID).getStatus());
        assertEquals(new BigDecimal("99.00"), skuStore.get(SKU_ID).getPrice().amount());
    }

    @Test
    @DisplayName("Guard does not save for ACTIVE")
    void active_guardDoesNotSave() {
        FakeBaseStore baseStore = new FakeBaseStore();
        baseStore.put(withStatus(BASE_ID, ProductStatus.ACTIVE));
        FakeSkuStore skuStore = new FakeSkuStore();
        skuStore.put(sku(SKU_ID, VARIANT_ID));

        service(baseStore, skuStore).execute(SKU_ID, new Money(new BigDecimal("99.00")), ADMIN_ACTOR);

        assertTrue(baseStore.saved.isEmpty());
    }

    @Test
    @DisplayName("Guard saves once for PENDING_REVIEW")
    void pendingReview_guardSavesOnce() {
        FakeBaseStore baseStore = new FakeBaseStore();
        baseStore.put(withStatus(BASE_ID, ProductStatus.PENDING_REVIEW));
        FakeSkuStore skuStore = new FakeSkuStore();
        skuStore.put(sku(SKU_ID, VARIANT_ID));

        service(baseStore, skuStore).execute(SKU_ID, new Money(new BigDecimal("49.99")), ADMIN_ACTOR);

        assertEquals(1, baseStore.saved.size());
    }

    @Test
    @DisplayName("Unknown SKU → IllegalArgumentException")
    void unknownSku_throws() {
        FakeBaseStore baseStore = new FakeBaseStore();
        baseStore.put(withStatus(BASE_ID, ProductStatus.ACTIVE));
        FakeSkuStore skuStore = new FakeSkuStore();

        // Owner port returns empty for any SKU not in the store
        FakeSkuOwnerPort ownerPort = new FakeSkuOwnerPort(null) {
            @Override public Optional<SkuOwner> findBySkuId(Long id) { return Optional.empty(); }
        };

        assertThrows(IllegalArgumentException.class,
                () -> service(baseStore, skuStore, ownerPort)
                        .execute(SKU_ID, new Money(new BigDecimal("10.00")), ADMIN_ACTOR));
    }

    // ── Ownership guard tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Seller updates price of their own SKU → succeeds")
    void seller_own_sku_priceUpdated() {
        FakeBaseStore baseStore = new FakeBaseStore();
        baseStore.put(withStatus(BASE_ID, ProductStatus.ACTIVE));
        FakeSkuStore skuStore = new FakeSkuStore();
        skuStore.put(sku(SKU_ID, VARIANT_ID));

        SkuEditActor sellerActor = new SkuEditActor(UserRole.SELLER, 99L, SELLER_A);
        service(baseStore, skuStore, new FakeSkuOwnerPort(SELLER_A))
                .execute(SKU_ID, new Money(new BigDecimal("55.00")), sellerActor);

        assertEquals(new BigDecimal("55.00"), skuStore.get(SKU_ID).getPrice().amount());
    }

    @Test
    @DisplayName("Seller updates price of another seller's SKU → FieldLockException, nothing saved")
    void seller_foreign_sku_fieldLock() {
        FakeBaseStore baseStore = new FakeBaseStore();
        baseStore.put(withStatus(BASE_ID, ProductStatus.ACTIVE));
        FakeSkuStore skuStore = new FakeSkuStore();
        skuStore.put(sku(SKU_ID, VARIANT_ID));

        SkuEditActor sellerActor = new SkuEditActor(UserRole.SELLER, 99L, SELLER_A);
        assertThrows(FieldLockException.class, () ->
                service(baseStore, skuStore, new FakeSkuOwnerPort(SELLER_B))
                        .execute(SKU_ID, new Money(new BigDecimal("55.00")), sellerActor));

        // Price must not have changed
        assertEquals(new BigDecimal("29.99"), skuStore.get(SKU_ID).getPrice().amount());
    }

    @Test
    @DisplayName("Seller updates price of Radolfa-owned SKU → FieldLockException, nothing saved")
    void seller_radolfa_sku_fieldLock() {
        FakeBaseStore baseStore = new FakeBaseStore();
        baseStore.put(withStatus(BASE_ID, ProductStatus.ACTIVE));
        FakeSkuStore skuStore = new FakeSkuStore();
        skuStore.put(sku(SKU_ID, VARIANT_ID));

        SkuEditActor sellerActor = new SkuEditActor(UserRole.SELLER, 99L, SELLER_A);
        // ownerSellerId=null → Radolfa-owned
        assertThrows(FieldLockException.class, () ->
                service(baseStore, skuStore, new FakeSkuOwnerPort(null))
                        .execute(SKU_ID, new Money(new BigDecimal("55.00")), sellerActor));

        assertEquals(new BigDecimal("29.99"), skuStore.get(SKU_ID).getPrice().amount());
    }
}
