package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.product.ProductActor;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.FieldLockException;
import tj.radolfa.domain.exception.IllegalProductStatusTransitionException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.UserRole;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SubmitProductForReviewServiceTest {

    private static final Long SELLER_A = 10L;
    private static final Long SELLER_B = 20L;

    private static final ProductActor ADMIN_ACTOR   =
            new ProductActor(UserRole.ADMIN,   1L, null);
    private static final ProductActor MANAGER_ACTOR =
            new ProductActor(UserRole.MANAGER, 2L, null);

    // ── Shared fakes ──────────────────────────────────────────────────────────

    static class FakeStore implements LoadProductBasePort, SaveProductHierarchyPort {
        final Map<Long, ProductBase> store = new HashMap<>();

        void put(ProductBase pb) { store.put(pb.getId(), pb); }
        ProductBase get(Long id) { return store.get(id); }

        @Override public Optional<ProductBase> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<ProductBase> findByExternalRef(String r) { return Optional.empty(); }
        @Override public Map<Long, ProductBase> findProductsByIds(Collection<Long> ids) { return Map.of(); }

        @Override public ProductBase saveBase(ProductBase base) { store.put(base.getId(), base); return base; }
        @Override public ListingVariant saveVariant(ListingVariant v, Long id) { return v; }
        @Override public Sku saveSku(Sku s, Long id) { return s; }
    }

    static SubmitProductForReviewService service(FakeStore store) {
        return new SubmitProductForReviewService(store, store);
    }

    /** A DRAFT product owned by Radolfa (no seller). */
    static ProductBase draft(Long id) {
        return new ProductBase(id, "EXT-" + id, "Name", null, null, null, ProductStatus.DRAFT, null);
    }

    static ProductBase withStatus(Long id, ProductStatus status, String reason) {
        return new ProductBase(id, "EXT-" + id, "Name", null, null, null, status, reason);
    }

    /** A DRAFT product owned by the given seller. */
    static ProductBase draftOwnedBy(Long id, Long sellerId) {
        return new ProductBase(id, "EXT-" + id, "Name", null, null, null,
                ProductStatus.DRAFT, null, sellerId);
    }

    // ── Existing lifecycle tests (unchanged behaviour, actors updated) ─────────

    @Test
    @DisplayName("DRAFT → PENDING_REVIEW (ADMIN actor)")
    void draft_submits_to_pendingReview() {
        FakeStore store = new FakeStore();
        store.put(draft(1L));

        service(store).execute(1L, ADMIN_ACTOR);

        assertEquals(ProductStatus.PENDING_REVIEW, store.get(1L).getStatus());
    }

    @Test
    @DisplayName("REJECTED → PENDING_REVIEW; rejectionReason cleared (ADMIN actor)")
    void rejected_submits_and_clears_reason() {
        FakeStore store = new FakeStore();
        store.put(withStatus(1L, ProductStatus.REJECTED, "Bad images"));

        service(store).execute(1L, ADMIN_ACTOR);

        ProductBase result = store.get(1L);
        assertEquals(ProductStatus.PENDING_REVIEW, result.getStatus());
        assertNull(result.getRejectionReason());
    }

    @Test
    @DisplayName("PENDING_REVIEW → IllegalProductStatusTransitionException (ADMIN actor)")
    void pendingReview_throws() {
        FakeStore store = new FakeStore();
        store.put(withStatus(1L, ProductStatus.PENDING_REVIEW, null));

        assertThrows(IllegalProductStatusTransitionException.class,
                () -> service(store).execute(1L, ADMIN_ACTOR));
    }

    @Test
    @DisplayName("ACTIVE → IllegalProductStatusTransitionException (ADMIN actor)")
    void active_throws() {
        FakeStore store = new FakeStore();
        store.put(withStatus(1L, ProductStatus.ACTIVE, null));

        assertThrows(IllegalProductStatusTransitionException.class,
                () -> service(store).execute(1L, ADMIN_ACTOR));
    }

    @Test
    @DisplayName("Unknown product → ResourceNotFoundException")
    void unknownProduct_throws() {
        FakeStore store = new FakeStore();
        assertThrows(ResourceNotFoundException.class, () -> service(store).execute(999L, ADMIN_ACTOR));
    }

    // ── MANAGER: allowed on any product ───────────────────────────────────────

    @Test
    @DisplayName("MANAGER submits Radolfa product → PENDING_REVIEW")
    void manager_radolfa_product_allowed() {
        FakeStore store = new FakeStore();
        store.put(draft(1L)); // Radolfa-owned

        service(store).execute(1L, MANAGER_ACTOR);

        assertEquals(ProductStatus.PENDING_REVIEW, store.get(1L).getStatus());
    }

    @Test
    @DisplayName("MANAGER submits seller-owned product → PENDING_REVIEW")
    void manager_seller_product_allowed() {
        FakeStore store = new FakeStore();
        store.put(draftOwnedBy(1L, SELLER_A));

        service(store).execute(1L, MANAGER_ACTOR);

        assertEquals(ProductStatus.PENDING_REVIEW, store.get(1L).getStatus());
    }

    // ── SELLER: allowed only on their own products ────────────────────────────

    @Test
    @DisplayName("SELLER submits their own product → PENDING_REVIEW")
    void seller_own_product_allowed() {
        FakeStore store = new FakeStore();
        store.put(draftOwnedBy(1L, SELLER_A));
        ProductActor sellerActor = new ProductActor(UserRole.SELLER, 99L, SELLER_A);

        service(store).execute(1L, sellerActor);

        assertEquals(ProductStatus.PENDING_REVIEW, store.get(1L).getStatus());
    }

    @Test
    @DisplayName("SELLER submits another seller's product → FieldLockException, status unchanged")
    void seller_foreign_product_denied() {
        FakeStore store = new FakeStore();
        store.put(draftOwnedBy(1L, SELLER_B)); // owned by B
        ProductActor sellerA = new ProductActor(UserRole.SELLER, 99L, SELLER_A);

        assertThrows(FieldLockException.class,
                () -> service(store).execute(1L, sellerA));

        // Status must not have changed
        assertEquals(ProductStatus.DRAFT, store.get(1L).getStatus());
    }

    @Test
    @DisplayName("SELLER submits a Radolfa-owned product → FieldLockException, status unchanged")
    void seller_radolfa_product_denied() {
        FakeStore store = new FakeStore();
        store.put(draft(1L)); // Radolfa-owned (sellerId=null)
        ProductActor sellerA = new ProductActor(UserRole.SELLER, 99L, SELLER_A);

        assertThrows(FieldLockException.class,
                () -> service(store).execute(1L, sellerA));

        assertEquals(ProductStatus.DRAFT, store.get(1L).getStatus());
    }
}
