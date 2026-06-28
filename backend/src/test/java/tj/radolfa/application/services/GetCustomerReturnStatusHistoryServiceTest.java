package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tj.radolfa.application.ports.out.LoadCustomerReturnStatusChangePort;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.CustomerReturnStatusChange;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetCustomerReturnStatusHistoryServiceTest {

    // ── Fake ──────────────────────────────────────────────────────────────────

    static class FakeLoadCustomerReturnStatusChangePort implements LoadCustomerReturnStatusChangePort {
        final List<CustomerReturnStatusChange> rows = new ArrayList<>();
        Pageable lastPageable;

        @Override
        public Page<CustomerReturnStatusChange> findByReturnId(Long returnId, Pageable pageable) {
            this.lastPageable = pageable;
            List<CustomerReturnStatusChange> matching = rows.stream()
                    .filter(r -> r.returnId().equals(returnId))
                    .toList();
            int start = (int) pageable.getOffset();
            int end   = Math.min(start + pageable.getPageSize(), matching.size());
            List<CustomerReturnStatusChange> slice = start >= matching.size()
                    ? List.of() : matching.subList(start, end);
            return new PageImpl<>(slice, pageable, matching.size());
        }
    }

    static CustomerReturnStatusChange row(Long returnId, CustomerReturnStatus from,
                                          CustomerReturnStatus to, Long actorUserId) {
        return new CustomerReturnStatusChange(null, returnId, from, to, actorUserId, null, Instant.now());
    }

    static GetCustomerReturnStatusHistoryService service(FakeLoadCustomerReturnStatusChangePort port) {
        return new GetCustomerReturnStatusHistoryService(port);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Returns all rows for the given returnId, page-wrapped")
    void returnsRowsForReturnId() {
        var port = new FakeLoadCustomerReturnStatusChangePort();
        port.rows.add(row(1L, null,                           CustomerReturnStatus.RECEIVED,          10L));
        port.rows.add(row(1L, CustomerReturnStatus.RECEIVED,  CustomerReturnStatus.SENT_TO_WAREHOUSE, 10L));
        port.rows.add(row(2L, null,                           CustomerReturnStatus.RECEIVED,          11L)); // different return

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<CustomerReturnStatusChange> result = service(port).execute(1L, pageable);

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(r -> r.returnId().equals(1L)));
    }

    @Test
    @DisplayName("Delegates Pageable unchanged to the port")
    void delegatesPageableToPort() {
        var port = new FakeLoadCustomerReturnStatusChangePort();
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "occurredAt"));

        service(port).execute(1L, pageable);

        assertSame(pageable, port.lastPageable);
    }

    @Test
    @DisplayName("Empty result when no rows exist for returnId")
    void emptyResultWhenNoRows() {
        var port = new FakeLoadCustomerReturnStatusChangePort();
        Pageable pageable = PageRequest.of(0, 20);

        Page<CustomerReturnStatusChange> result = service(port).execute(999L, pageable);

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("Pagination: page 1 of size 1 returns only the second row")
    void pagination_secondPage() {
        var port = new FakeLoadCustomerReturnStatusChangePort();
        port.rows.add(row(1L, null,                          CustomerReturnStatus.RECEIVED,          10L));
        port.rows.add(row(1L, CustomerReturnStatus.RECEIVED, CustomerReturnStatus.SENT_TO_WAREHOUSE, 10L));

        Pageable pageable = PageRequest.of(1, 1);
        Page<CustomerReturnStatusChange> result = service(port).execute(1L, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(CustomerReturnStatus.SENT_TO_WAREHOUSE, result.getContent().get(0).statusTo());
    }
}
