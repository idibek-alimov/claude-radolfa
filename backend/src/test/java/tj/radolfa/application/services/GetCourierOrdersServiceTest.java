package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadCourierOrdersPort;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetCourierOrdersServiceTest {

    // ── Fake ─────────────────────────────────────────────────────────────────

    static class CapturingLoadCourierOrdersPort implements LoadCourierOrdersPort {
        List<OrderStatus> capturedStatuses;
        int capturedPage;
        int capturedSize;
        PageResult<Order> result = new PageResult<>(List.of(), 0, 1, 20, true);

        @Override
        public List<Order> loadByCourierIdAndStatuses(Long id, List<OrderStatus> s) {
            return result.content();
        }

        @Override
        public PageResult<Order> loadByCourierIdAndStatusesPaged(Long id,
                                                                  List<OrderStatus> s,
                                                                  int p, int sz) {
            this.capturedStatuses = new ArrayList<>(s);
            this.capturedPage     = p;
            this.capturedSize     = sz;
            return result;
        }
    }

    static Order order(Long id, OrderStatus status) {
        return new Order.Builder()
                .id(id).userId(1L).status(status)
                .totalAmount(new Money(BigDecimal.ZERO)).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).build();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Passes caller-supplied statuses and pagination params to the port")
    void execute_passesStatusesAndPageParams() {
        var port = new CapturingLoadCourierOrdersPort();
        var svc  = new GetCourierOrdersService(port);

        svc.execute(99L, List.of(OrderStatus.SHIPPED), 1, 10);

        assertEquals(List.of(OrderStatus.SHIPPED), port.capturedStatuses);
        assertEquals(1,  port.capturedPage);
        assertEquals(10, port.capturedSize);
    }

    @Test
    @DisplayName("Multiple statuses forwarded correctly")
    void execute_multipleStatuses() {
        var port = new CapturingLoadCourierOrdersPort();
        var svc  = new GetCourierOrdersService(port);

        var statuses = List.of(
                OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERY_ATTEMPTED);
        svc.execute(99L, statuses, 1, 20);

        assertTrue(port.capturedStatuses.contains(OrderStatus.SHIPPED));
        assertTrue(port.capturedStatuses.contains(OrderStatus.OUT_FOR_DELIVERY));
        assertTrue(port.capturedStatuses.contains(OrderStatus.DELIVERY_ATTEMPTED));
    }

    @Test
    @DisplayName("Page 2 with size 5 forwarded to port correctly")
    void execute_page2Size5() {
        var port = new CapturingLoadCourierOrdersPort();
        var svc  = new GetCourierOrdersService(port);

        svc.execute(99L, List.of(OrderStatus.OUT_FOR_DELIVERY), 2, 5);

        assertEquals(2, port.capturedPage);
        assertEquals(5, port.capturedSize);
    }

    @Test
    @DisplayName("Service returns PageResult from port unchanged")
    void execute_returnsPortResultUnchanged() {
        var port = new CapturingLoadCourierOrdersPort();
        var o1 = order(1L, OrderStatus.SHIPPED);
        port.result = new PageResult<>(List.of(o1), 3, 2, 2, true);
        var svc = new GetCourierOrdersService(port);

        var result = svc.execute(99L, List.of(OrderStatus.SHIPPED), 2, 2);

        assertEquals(1,  result.content().size());
        assertEquals(3L, result.totalElements());
        assertEquals(2,  result.number());
        assertEquals(2,  result.size());
        assertTrue(result.last());
    }

    @Test
    @DisplayName("Empty result from port → empty PageResult returned")
    void execute_emptyResult() {
        var port = new CapturingLoadCourierOrdersPort();
        var svc  = new GetCourierOrdersService(port);

        var result = svc.execute(99L, List.of(OrderStatus.SHIPPED), 1, 20);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }
}
