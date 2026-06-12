package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadSellerPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Seller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GetMySellerProfileService}.
 * No Spring context, no Mockito, no database — all dependencies are hand-written fakes.
 */
class GetMySellerProfileServiceTest {

    @Test
    @DisplayName("Seller profile exists → returned as-is")
    void getProfile_exists_returnsProfile() {
        var store = new FakeLoadSellerPort();
        var seller = new Seller(1L, 42L, "My Shop", null, null, null);
        store.store(seller);
        var svc = new GetMySellerProfileService(store);

        Seller result = svc.execute(42L);

        assertEquals(1L, result.id());
        assertEquals(42L, result.userId());
        assertEquals("My Shop", result.shopName());
    }

    @Test
    @DisplayName("No seller profile for user → ResourceNotFoundException")
    void getProfile_missing_throws() {
        var store = new FakeLoadSellerPort();
        var svc = new GetMySellerProfileService(store);

        assertThrows(ResourceNotFoundException.class, () -> svc.execute(99L));
    }

    // ----------------------------------------------------------------
    // Fake
    // ----------------------------------------------------------------

    static class FakeLoadSellerPort implements LoadSellerPort {
        private final Map<Long, Seller> byUserId = new HashMap<>();

        void store(Seller s) { byUserId.put(s.userId(), s); }

        @Override public Optional<Seller> findByUserId(Long userId) {
            return Optional.ofNullable(byUserId.get(userId));
        }
        @Override public Optional<Seller> findById(Long id) { return Optional.empty(); }
        @Override public PageResult<Seller> findAllPaged(int page, int size, String search) {
            return new PageResult<>(java.util.List.of(), 0, page, size, true);
        }
    }
}
