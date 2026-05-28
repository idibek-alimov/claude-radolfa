package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.product.RejectProductUseCase;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RejectProductServiceTest {

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

    static RejectProductService service(FakeStore store) {
        return new RejectProductService(store, store);
    }

    static ProductBase withStatus(Long id, ProductStatus status) {
        return new ProductBase(id, "EXT-" + id, "Name", null, null, null, status, null);
    }

    @Test
    @DisplayName("PENDING_REVIEW → REJECTED; reason stored")
    void pendingReview_rejectsWithReason() {
        FakeStore store = new FakeStore();
        store.put(withStatus(1L, ProductStatus.PENDING_REVIEW));

        service(store).execute(new RejectProductUseCase.Command(1L, "Missing images", 10L));

        ProductBase result = store.get(1L);
        assertEquals(ProductStatus.REJECTED, result.getStatus());
        assertEquals("Missing images", result.getRejectionReason());
    }

    @Test
    @DisplayName("Blank rejection reason → IllegalArgumentException")
    void blankReason_throws() {
        FakeStore store = new FakeStore();
        store.put(withStatus(1L, ProductStatus.PENDING_REVIEW));

        assertThrows(IllegalArgumentException.class,
                () -> service(store).execute(new RejectProductUseCase.Command(1L, "  ", 10L)));
    }

    @Test
    @DisplayName("DRAFT → IllegalProductStatusTransitionException")
    void draft_throws() {
        FakeStore store = new FakeStore();
        store.put(withStatus(1L, ProductStatus.DRAFT));

        assertThrows(IllegalProductStatusTransitionException.class,
                () -> service(store).execute(new RejectProductUseCase.Command(1L, "reason", 10L)));
    }

    @Test
    @DisplayName("Unknown product → ResourceNotFoundException")
    void unknownProduct_throws() {
        FakeStore store = new FakeStore();
        assertThrows(ResourceNotFoundException.class,
                () -> service(store).execute(new RejectProductUseCase.Command(999L, "reason", 1L)));
    }
}
