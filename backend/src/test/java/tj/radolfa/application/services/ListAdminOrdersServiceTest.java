package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadAdminOrdersPort;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListAdminOrdersServiceTest {

    private static Order order(Long id) {
        return new Order.Builder()
                .id(id).userId(1L).status(OrderStatus.PENDING)
                .totalAmount(new Money(BigDecimal.TEN)).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
                .build();
    }

    private static LoadAdminOrdersPort.OrderRow row(Long id) {
        return new LoadAdminOrdersPort.OrderRow(order(id), "992001", "Alice");
    }

    // ── Sort-whitelist guard ──────────────────────────────────────────────────

    @Test
    @DisplayName("Unknown sortBy falls back to createdAt")
    void unknownSortBy_fallsBackToCreatedAt() {
        String[] capturedSortBy = new String[1];
        LoadAdminOrdersPort spy = (search, statusFilter, sortBy, sortDir, page, size) -> {
            capturedSortBy[0] = sortBy;
            return new PageResult<>(List.of(), 0, page, size, true);
        };

        ListAdminOrdersService service = new ListAdminOrdersService(spy);
        service.execute("", null, "DROP TABLE", "DESC", 1, 20);

        assertEquals("createdAt", capturedSortBy[0]);
    }

    @Test
    @DisplayName("Valid sortBy (totalAmount) is forwarded as-is")
    void validSortBy_forwarded() {
        String[] capturedSortBy = new String[1];
        LoadAdminOrdersPort spy = (search, statusFilter, sortBy, sortDir, page, size) -> {
            capturedSortBy[0] = sortBy;
            return new PageResult<>(List.of(), 0, page, size, true);
        };

        ListAdminOrdersService service = new ListAdminOrdersService(spy);
        service.execute("", null, "totalAmount", "ASC", 1, 20);

        assertEquals("totalAmount", capturedSortBy[0]);
    }

    @Test
    @DisplayName("sortDir defaults to DESC when unknown value is passed")
    void unknownSortDir_defaultsToDesc() {
        String[] capturedDir = new String[1];
        LoadAdminOrdersPort spy = (search, statusFilter, sortBy, sortDir, page, size) -> {
            capturedDir[0] = sortDir;
            return new PageResult<>(List.of(), 0, page, size, true);
        };

        ListAdminOrdersService service = new ListAdminOrdersService(spy);
        service.execute("", null, "createdAt", "RANDOM", 1, 20);

        assertEquals("DESC", capturedDir[0]);
    }

    // ── Size cap ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Size cap enforced at 100")
    void sizeCap_enforcedAt100() {
        int[] capturedSize = new int[1];
        LoadAdminOrdersPort spy = (search, statusFilter, sortBy, sortDir, page, size) -> {
            capturedSize[0] = size;
            return new PageResult<>(List.of(), 0, page, size, true);
        };

        ListAdminOrdersService service = new ListAdminOrdersService(spy);
        service.execute("", null, "createdAt", "DESC", 1, 9999);

        assertEquals(100, capturedSize[0]);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Result is passed through from port unchanged")
    void resultPassedThrough() {
        List<LoadAdminOrdersPort.OrderRow> rows = List.of(row(1L), row(2L));
        LoadAdminOrdersPort port = (search, statusFilter, sortBy, sortDir, page, size) ->
                new PageResult<>(rows, 2, page, size, true);

        ListAdminOrdersService service = new ListAdminOrdersService(port);
        PageResult<LoadAdminOrdersPort.OrderRow> result =
                service.execute("", null, "createdAt", "DESC", 1, 20);

        assertEquals(2, result.totalElements());
        assertEquals(2, result.content().size());
        assertEquals(1L, result.content().get(0).order().id());
    }
}
