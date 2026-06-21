package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadAdminOrdersPort;
import tj.radolfa.application.readmodel.PickQueueItem;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetWarehousePickQueueServiceTest {

    // ── Fixtures ──────────────────────────────────────────────────────────────

    static OrderItem item(Long id, int qty, int picked) {
        return new OrderItem(id, null, null, "SKU-" + id, "Product " + id, qty,
                new Money(BigDecimal.TEN), picked, null, null, null);
    }

    static Order paidOrder(Long orderId, DeliveryType deliveryType, List<OrderItem> items) {
        return new Order.Builder()
                .id(orderId).userId(1L).externalOrderId("ORD-" + orderId)
                .status(OrderStatus.PAID).deliveryType(deliveryType)
                .items(items).createdAt(Instant.now())
                .build();
    }

    // ── Fake ──────────────────────────────────────────────────────────────────

    static class CapturingLoadAdminOrdersPort implements LoadAdminOrdersPort {
        record SearchCall(String search, Collection<OrderStatus> statuses,
                          String sortBy, String sortDir, int page, int size) {}
        final List<SearchCall> calls = new ArrayList<>();
        PageResult<OrderRow> result = new PageResult<>(List.of(), 0, 1, 20, true);

        @Override
        public PageResult<OrderRow> search(String search, Collection<OrderStatus> statuses,
                                           String sortBy, String sortDir, int page, int size) {
            calls.add(new SearchCall(search, statuses, sortBy, sortDir, page, size));
            return result;
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Passes PAID and AWAITING_COD statuses and createdAt ASC sort to the port")
    void delegatesCorrectParamsToPort() {
        var port = new CapturingLoadAdminOrdersPort();
        var svc  = new GetWarehousePickQueueService(port);

        svc.execute(2, 10, "ORD-");

        assertEquals(1, port.calls.size());
        var call = port.calls.get(0);
        assertEquals("ORD-", call.search());
        assertTrue(call.statuses().contains(OrderStatus.PAID));
        assertTrue(call.statuses().contains(OrderStatus.AWAITING_COD));
        assertEquals(2, call.statuses().size(), "should filter by PAID and AWAITING_COD only");
        assertEquals("createdAt", call.sortBy());
        assertEquals("ASC", call.sortDir());
        assertEquals(2, call.page());
        assertEquals(10, call.size());
    }

    @Test
    @DisplayName("totalUnits and pickedUnits are summed correctly across all order items")
    void totalsAreAggregatedCorrectly() {
        OrderItem i1 = item(1L, 2, 1); // 2 total, 1 picked
        OrderItem i2 = item(2L, 3, 3); // 3 total, 3 picked
        Order order = paidOrder(10L, DeliveryType.HOME, List.of(i1, i2));

        var port = new CapturingLoadAdminOrdersPort();
        port.result = new PageResult<>(
                List.of(new LoadAdminOrdersPort.OrderRow(order, "+99000000000", "Alice")),
                1, 1, 20, true);

        var svc = new GetWarehousePickQueueService(port);
        PageResult<PickQueueItem> result = svc.execute(1, 20, null);

        assertEquals(1, result.content().size());
        PickQueueItem item = result.content().get(0);
        assertEquals(10L, item.orderId());
        assertEquals("ORD-10", item.externalOrderId());
        assertEquals(DeliveryType.HOME, item.deliveryType());
        assertEquals("Alice", item.customerName());
        assertEquals(5, item.totalUnits());
        assertEquals(4, item.pickedUnits());
    }

    @Test
    @DisplayName("Pagination metadata is echoed from the port result")
    void paginationMetadataEchoed() {
        var port = new CapturingLoadAdminOrdersPort();
        port.result = new PageResult<>(List.of(), 42L, 3, 10, false);

        var svc = new GetWarehousePickQueueService(port);
        PageResult<PickQueueItem> result = svc.execute(3, 10, null);

        assertEquals(42L, result.totalElements());
        assertEquals(3, result.number());
        assertEquals(10, result.size());
        assertFalse(result.last());
    }
}
