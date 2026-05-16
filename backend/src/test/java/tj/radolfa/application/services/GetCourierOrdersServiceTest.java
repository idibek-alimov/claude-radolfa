package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadCourierOrdersPort;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetCourierOrdersServiceTest {

    static class CapturingLoadCourierOrdersPort implements LoadCourierOrdersPort {
        List<OrderStatus> capturedStatuses;
        List<Order> result = List.of();

        @Override
        public List<Order> loadByCourierIdAndStatuses(Long courierId, List<OrderStatus> statuses) {
            this.capturedStatuses = new ArrayList<>(statuses);
            return result;
        }
    }

    static Order order(Long id, OrderStatus status, Instant createdAt) {
        return new Order.Builder()
                .id(id).userId(1L).status(status)
                .totalAmount(new Money(BigDecimal.ZERO)).createdAt(createdAt)
                .deliveryType(DeliveryType.HOME).build();
    }

    @Test
    @DisplayName("Fetches with statuses DELIVERY_ATTEMPTED, OUT_FOR_DELIVERY, SHIPPED")
    void execute_callsPortWithCorrectStatuses() {
        var port = new CapturingLoadCourierOrdersPort();
        var svc  = new GetCourierOrdersService(port);

        svc.execute(99L);

        assertTrue(port.capturedStatuses.contains(OrderStatus.DELIVERY_ATTEMPTED));
        assertTrue(port.capturedStatuses.contains(OrderStatus.OUT_FOR_DELIVERY));
        assertTrue(port.capturedStatuses.contains(OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("Sort order: DELIVERY_ATTEMPTED first, then OUT_FOR_DELIVERY, then SHIPPED; ties by createdAt asc")
    void execute_sortOrderCorrect() {
        var port = new CapturingLoadCourierOrdersPort();
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-02T00:00:00Z");
        Instant t3 = Instant.parse("2026-01-03T00:00:00Z");

        port.result = List.of(
                order(3L, OrderStatus.SHIPPED, t1),
                order(1L, OrderStatus.DELIVERY_ATTEMPTED, t2),
                order(2L, OrderStatus.OUT_FOR_DELIVERY, t3)
        );
        var svc = new GetCourierOrdersService(port);

        List<Order> result = svc.execute(99L);

        assertEquals(3, result.size());
        assertEquals(OrderStatus.DELIVERY_ATTEMPTED, result.get(0).status());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY,   result.get(1).status());
        assertEquals(OrderStatus.SHIPPED,            result.get(2).status());
    }

    @Test
    @DisplayName("Same status sorted by createdAt ascending")
    void execute_sameStatusSortedByCreatedAt() {
        var port = new CapturingLoadCourierOrdersPort();
        Instant early = Instant.parse("2026-01-01T00:00:00Z");
        Instant late  = Instant.parse("2026-01-05T00:00:00Z");

        port.result = List.of(
                order(2L, OrderStatus.SHIPPED, late),
                order(1L, OrderStatus.SHIPPED, early)
        );
        var svc = new GetCourierOrdersService(port);

        List<Order> result = svc.execute(99L);

        assertEquals(1L, result.get(0).id());
        assertEquals(2L, result.get(1).id());
    }

    @Test
    @DisplayName("Empty order list returns empty list")
    void execute_empty() {
        var port = new CapturingLoadCourierOrdersPort();
        var svc  = new GetCourierOrdersService(port);

        List<Order> result = svc.execute(99L);

        assertTrue(result.isEmpty());
    }
}
