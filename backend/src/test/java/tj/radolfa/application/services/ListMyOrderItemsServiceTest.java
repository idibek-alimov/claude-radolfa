package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadSellerOrderItemsPort;
import tj.radolfa.application.readmodel.SellerOrderItemRow;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListMyOrderItemsServiceTest {

    // ── Fixtures ──────────────────────────────────────────────────────────────

    static final Long SELLER_A = 1L;
    static final Long SELLER_B = 2L;

    static SellerOrderItemRow row(Long orderItemId, Long sellerId) {
        return new SellerOrderItemRow(
                orderItemId, 100L + orderItemId, OrderStatus.PAID,
                Instant.now(), "Product " + orderItemId, "SKU-" + orderItemId,
                2, BigDecimal.valueOf(50));
    }

    // ── Fake port ─────────────────────────────────────────────────────────────

    /**
     * Records the last invocation for assertion; returns only rows whose
     * orderItemId matches the sellerId (a test convention — in real queries the
     * DB filters by seller_id; here we just simulate isolation between sellers).
     */
    static class FakeLoadSellerOrderItemsPort implements LoadSellerOrderItemsPort {

        record Call(Long sellerId, String search, String sortBy, String sortDir, int page, int size) {}

        Call lastCall;
        final List<SellerOrderItemRow> sellerARows = List.of(row(1L, SELLER_A), row(2L, SELLER_A));
        final List<SellerOrderItemRow> sellerBRows = List.of(row(3L, SELLER_B));

        @Override
        public PageResult<SellerOrderItemRow> findBySellerId(Long sellerId, String search,
                                                              String sortBy, String sortDir,
                                                              int page, int size) {
            lastCall = new Call(sellerId, search, sortBy, sortDir, page, size);
            List<SellerOrderItemRow> content = SELLER_A.equals(sellerId) ? sellerARows
                    : SELLER_B.equals(sellerId) ? sellerBRows
                    : List.of();
            return new PageResult<>(content, content.size(), page, size, true);
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Seller A sees only their 2 rows — seller B's row is absent")
    void sellerA_sees_only_own_rows() {
        var port    = new FakeLoadSellerOrderItemsPort();
        var service = new ListMyOrderItemsService(port);

        PageResult<SellerOrderItemRow> result =
                service.execute(SELLER_A, "", "orderCreatedAt", "DESC", 1, 20);

        assertEquals(2, result.content().size());
        assertTrue(result.content().stream().noneMatch(r -> r.orderItemId().equals(3L)),
                "Seller B row must not appear in seller A results");
    }

    @Test
    @DisplayName("Seller B sees only their 1 row — seller A's rows are absent")
    void sellerB_sees_only_own_rows() {
        var port    = new FakeLoadSellerOrderItemsPort();
        var service = new ListMyOrderItemsService(port);

        PageResult<SellerOrderItemRow> result =
                service.execute(SELLER_B, "", "orderCreatedAt", "DESC", 1, 20);

        assertEquals(1, result.content().size());
        assertEquals(3L, result.content().get(0).orderItemId());
    }

    @Test
    @DisplayName("Unknown sortBy falls back to 'orderCreatedAt'")
    void unknown_sort_falls_back_to_default() {
        var port    = new FakeLoadSellerOrderItemsPort();
        var service = new ListMyOrderItemsService(port);

        service.execute(SELLER_A, "", "maliciousInput; DROP TABLE orders;--", "DESC", 1, 20);

        assertEquals("orderCreatedAt", port.lastCall.sortBy(),
                "Service must whitelist sort columns and replace unknown values with the default");
    }

    @Test
    @DisplayName("Invalid sortDir defaults to DESC")
    void invalid_sort_dir_defaults_to_desc() {
        var port    = new FakeLoadSellerOrderItemsPort();
        var service = new ListMyOrderItemsService(port);

        service.execute(SELLER_A, "", "orderCreatedAt", "INVALID", 1, 20);

        assertEquals("DESC", port.lastCall.sortDir());
    }

    @Test
    @DisplayName("Page clamped to minimum 1; size clamped to 1–100")
    void page_and_size_clamped() {
        var port    = new FakeLoadSellerOrderItemsPort();
        var service = new ListMyOrderItemsService(port);

        service.execute(SELLER_A, "", "orderCreatedAt", "DESC", -5, 999);

        assertEquals(1,   port.lastCall.page(), "page must be clamped to ≥ 1");
        assertEquals(100, port.lastCall.size(), "size must be clamped to ≤ 100");
    }

    @Test
    @DisplayName("Delegation — search and sortBy are passed through to the port unchanged (post-whitelist)")
    void delegation_params_pass_through() {
        var port    = new FakeLoadSellerOrderItemsPort();
        var service = new ListMyOrderItemsService(port);

        service.execute(SELLER_A, "widget", "orderStatus", "ASC", 2, 10);

        assertEquals(SELLER_A,      port.lastCall.sellerId());
        assertEquals("widget",      port.lastCall.search());
        assertEquals("orderStatus", port.lastCall.sortBy());
        assertEquals("ASC",         port.lastCall.sortDir());
        assertEquals(2,             port.lastCall.page());
        assertEquals(10,            port.lastCall.size());
    }
}
