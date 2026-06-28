package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tj.radolfa.application.ports.out.LoadOrderStatusChangePort;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.OrderStatusChange;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetOrderStatusHistoryServiceTest {

    // ── Fake ──────────────────────────────────────────────────────────────────

    static class FakeLoadOrderStatusChangePort implements LoadOrderStatusChangePort {
        final List<OrderStatusChange> rows = new ArrayList<>();
        Pageable lastPageable;

        @Override
        public Page<OrderStatusChange> findByOrderId(Long orderId, Pageable pageable) {
            this.lastPageable = pageable;
            List<OrderStatusChange> matching = rows.stream()
                    .filter(r -> r.orderId().equals(orderId))
                    .toList();
            int start = (int) pageable.getOffset();
            int end   = Math.min(start + pageable.getPageSize(), matching.size());
            List<OrderStatusChange> slice = start >= matching.size()
                    ? List.of() : matching.subList(start, end);
            return new PageImpl<>(slice, pageable, matching.size());
        }
    }

    static OrderStatusChange row(Long orderId, OrderStatus from, OrderStatus to, Long actorUserId) {
        return new OrderStatusChange(null, orderId, from, to, actorUserId, null, Instant.now());
    }

    static GetOrderStatusHistoryService service(FakeLoadOrderStatusChangePort port) {
        return new GetOrderStatusHistoryService(port);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Returns all rows for the given orderId, page-wrapped")
    void returnsRowsForOrderId() {
        FakeLoadOrderStatusChangePort port = new FakeLoadOrderStatusChangePort();
        port.rows.add(row(1L, null,                OrderStatus.PENDING,    null));
        port.rows.add(row(1L, OrderStatus.PENDING, OrderStatus.PAID,       null));
        port.rows.add(row(2L, null,                OrderStatus.PENDING,    null)); // different order

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<OrderStatusChange> result = service(port).execute(1L, pageable);

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(r -> r.orderId().equals(1L)));
    }

    @Test
    @DisplayName("Delegates Pageable unchanged to the port")
    void delegatesPageableToPort() {
        FakeLoadOrderStatusChangePort port = new FakeLoadOrderStatusChangePort();
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "occurredAt"));

        service(port).execute(1L, pageable);

        assertSame(pageable, port.lastPageable);
    }

    @Test
    @DisplayName("Empty result when no rows exist for orderId")
    void emptyResultWhenNoRows() {
        FakeLoadOrderStatusChangePort port = new FakeLoadOrderStatusChangePort();
        Pageable pageable = PageRequest.of(0, 20);

        Page<OrderStatusChange> result = service(port).execute(999L, pageable);

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("Pagination: page 1 of size 1 returns only the second row")
    void pagination_secondPage() {
        FakeLoadOrderStatusChangePort port = new FakeLoadOrderStatusChangePort();
        port.rows.add(row(1L, null,                OrderStatus.PENDING, null));
        port.rows.add(row(1L, OrderStatus.PENDING, OrderStatus.PAID,    null));

        Pageable pageable = PageRequest.of(1, 1); // page index 1 (second page), size 1
        Page<OrderStatusChange> result = service(port).execute(1L, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(OrderStatus.PAID, result.getContent().get(0).statusTo());
    }
}
