package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadAdminProductPagePort;
import tj.radolfa.application.readmodel.AdminProductRow;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.ProductStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ListAdminProductsService}.
 * No Spring context, no Mockito, no database — all dependencies are hand-written fakes.
 *
 * <p>Focus: the service correctly threads the {@code sellerId} filter through to the port
 * (so the server-side ownership isolation is not accidentally stripped out).
 */
class ListAdminProductsServiceTest {

    @Test
    @DisplayName("Admin view (sellerId=null) — port receives null, returns all products unchanged")
    void execute_adminView_sellerIdNullPassedThrough() {
        var port = new CapturingLoadAdminProductPagePort();
        var svc  = new ListAdminProductsService(port);

        svc.execute(ProductStatus.DRAFT, "query", 1, 10, null);

        assertNull(port.lastSellerId, "Admin view must pass sellerId=null to the port");
        assertEquals(ProductStatus.DRAFT, port.lastStatus);
        assertEquals("query", port.lastSearch);
        assertEquals(1, port.lastPage);
        assertEquals(10, port.lastSize);
    }

    @Test
    @DisplayName("Seller view (sellerId set) — port receives the concrete sellerId, enabling isolation")
    void execute_sellerView_sellerIdPassedThrough() {
        var port = new CapturingLoadAdminProductPagePort();
        var svc  = new ListAdminProductsService(port);

        svc.execute(null, "", 2, 20, 42L);

        assertEquals(42L, port.lastSellerId, "Seller view must pass sellerId=42 to the port");
        assertNull(port.lastStatus);
        assertEquals(2, port.lastPage);
    }

    @Test
    @DisplayName("Result from port is returned as-is (no transformation)")
    void execute_resultIsForwardedUnchanged() {
        var port   = new CapturingLoadAdminProductPagePort();
        var svc    = new ListAdminProductsService(port);
        var result = svc.execute(null, "", 1, 5, null);

        assertNotNull(result);
        assertTrue(result.content().isEmpty());
        assertEquals(0L, result.totalElements());
    }

    // ----------------------------------------------------------------
    // Fake
    // ----------------------------------------------------------------

    static class CapturingLoadAdminProductPagePort implements LoadAdminProductPagePort {
        ProductStatus lastStatus;
        String        lastSearch;
        int           lastPage;
        int           lastSize;
        Long          lastSellerId;

        @Override
        public PageResult<AdminProductRow> findAdminPage(ProductStatus status, String search,
                                                          int page, int size, Long sellerId) {
            this.lastStatus   = status;
            this.lastSearch   = search;
            this.lastPage     = page;
            this.lastSize     = size;
            this.lastSellerId = sellerId;
            return new PageResult<>(List.of(), 0L, page, size, true);
        }

        @Override
        public long countByStatus(ProductStatus status) {
            return 0;
        }
    }
}
