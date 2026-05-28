package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.IllegalProductStatusTransitionException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.domain.model.Sku;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SubmitProductForReviewServiceTest {

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

    static ProductBase draft(Long id) {
        return new ProductBase(id, "EXT-" + id, "Name", null, null, null, ProductStatus.DRAFT, null);
    }

    static ProductBase withStatus(Long id, ProductStatus status, String reason) {
        return new ProductBase(id, "EXT-" + id, "Name", null, null, null, status, reason);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DRAFT → PENDING_REVIEW")
    void draft_submits_to_pendingReview() {
        FakeStore store = new FakeStore();
        store.put(draft(1L));

        service(store).execute(1L, 42L);

        assertEquals(ProductStatus.PENDING_REVIEW, store.get(1L).getStatus());
    }

    @Test
    @DisplayName("REJECTED → PENDING_REVIEW; rejectionReason cleared")
    void rejected_submits_and_clears_reason() {
        FakeStore store = new FakeStore();
        store.put(withStatus(1L, ProductStatus.REJECTED, "Bad images"));

        service(store).execute(1L, 42L);

        ProductBase result = store.get(1L);
        assertEquals(ProductStatus.PENDING_REVIEW, result.getStatus());
        assertNull(result.getRejectionReason());
    }

    @Test
    @DisplayName("PENDING_REVIEW → IllegalProductStatusTransitionException")
    void pendingReview_throws() {
        FakeStore store = new FakeStore();
        store.put(withStatus(1L, ProductStatus.PENDING_REVIEW, null));

        assertThrows(IllegalProductStatusTransitionException.class,
                () -> service(store).execute(1L, 42L));
    }

    @Test
    @DisplayName("ACTIVE → IllegalProductStatusTransitionException")
    void active_throws() {
        FakeStore store = new FakeStore();
        store.put(withStatus(1L, ProductStatus.ACTIVE, null));

        assertThrows(IllegalProductStatusTransitionException.class,
                () -> service(store).execute(1L, 42L));
    }

    @Test
    @DisplayName("Unknown product → ResourceNotFoundException")
    void unknownProduct_throws() {
        FakeStore store = new FakeStore();
        assertThrows(ResourceNotFoundException.class, () -> service(store).execute(999L, 1L));
    }
}
