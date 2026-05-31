package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadProductCardPort;
import tj.radolfa.application.readmodel.ProductCardDto;
import tj.radolfa.domain.exception.FieldLockException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.ProductStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GetMyProductCardService}.
 * No Spring context, no Mockito — hand-written in-memory fakes.
 */
class GetMyProductCardServiceTest {

    static final Long PRODUCT_BASE_ID = 10L;
    static final Long SELLER_A        = 1L;
    static final Long SELLER_B        = 2L;

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeLoadProductBasePort implements LoadProductBasePort {
        private final ProductBase stored;
        FakeLoadProductBasePort(ProductBase stored) { this.stored = stored; }

        @Override
        public Optional<ProductBase> findById(Long id) {
            return PRODUCT_BASE_ID.equals(id) ? Optional.ofNullable(stored) : Optional.empty();
        }
        @Override
        public Optional<ProductBase> findByExternalRef(String ref) { return Optional.empty(); }
        @Override
        public Map<Long, ProductBase> findProductsByIds(Collection<Long> ids) { return Map.of(); }
    }

    static class FakeLoadProductCardPort implements LoadProductCardPort {
        private final boolean found;
        FakeLoadProductCardPort(boolean found) { this.found = found; }

        @Override
        public Optional<ProductCardDto> loadByProductBaseId(Long id) {
            if (!found) return Optional.empty();
            return Optional.of(new ProductCardDto(
                    PRODUCT_BASE_ID, "Test Product", null, null, null,
                    List.of(), ProductStatus.DRAFT, null));
        }
    }

    /** Build a seller-owned ProductBase. */
    static ProductBase sellerOwnedProduct(Long sellerId) {
        return new ProductBase(PRODUCT_BASE_ID, "EXT-001", "Test Product",
                null, null, null, ProductStatus.DRAFT, null, sellerId);
    }

    /** Build a Radolfa-owned (null sellerId) ProductBase. */
    static ProductBase radolfaOwnedProduct() {
        return new ProductBase(PRODUCT_BASE_ID, "EXT-001", "Test Product",
                null, null, null, ProductStatus.DRAFT, null, null);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Seller loads their own product — returns full card")
    void seller_loads_own_product_returns_card() {
        var base = sellerOwnedProduct(SELLER_A);
        var service = new GetMyProductCardService(
                new FakeLoadProductBasePort(base),
                new FakeLoadProductCardPort(true));

        ProductCardDto result = service.execute(PRODUCT_BASE_ID, SELLER_A);
        assertEquals(PRODUCT_BASE_ID, result.productBaseId());
    }

    @Test
    @DisplayName("Seller loads another seller's product → FieldLockException")
    void seller_cannot_load_other_sellers_product() {
        var base = sellerOwnedProduct(SELLER_B);
        var service = new GetMyProductCardService(
                new FakeLoadProductBasePort(base),
                new FakeLoadProductCardPort(true));

        assertThrows(FieldLockException.class,
                () -> service.execute(PRODUCT_BASE_ID, SELLER_A));
    }

    @Test
    @DisplayName("Seller loads a Radolfa-owned product (sellerId=null) → FieldLockException")
    void seller_cannot_load_radolfa_product() {
        var base = radolfaOwnedProduct();
        var service = new GetMyProductCardService(
                new FakeLoadProductBasePort(base),
                new FakeLoadProductCardPort(true));

        assertThrows(FieldLockException.class,
                () -> service.execute(PRODUCT_BASE_ID, SELLER_A));
    }

    @Test
    @DisplayName("Product not found → ResourceNotFoundException")
    void missing_product_throws_not_found() {
        var service = new GetMyProductCardService(
                new FakeLoadProductBasePort(null), // findById returns empty
                new FakeLoadProductCardPort(false));

        assertThrows(ResourceNotFoundException.class,
                () -> service.execute(PRODUCT_BASE_ID, SELLER_A));
    }
}
