package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.PageResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetMyReturnsServiceTest {

    static CustomerReturn fakeReturn(Long id) {
        return new CustomerReturn(id, 1L, 5L, 55L, Instant.now(), null,
                List.of(), CustomerReturnStatus.RECEIVED, null, null, null, null, null, null);
    }

    static class FakeLoadCustomerReturnPort implements LoadCustomerReturnPort {
        private final List<CustomerReturn> all;
        FakeLoadCustomerReturnPort(List<CustomerReturn> all) { this.all = all; }

        @Override
        public PageResult<CustomerReturn> loadByUserId(Long userId, int page, int size) {
            int from = Math.min((page - 1) * size, all.size());
            int to   = Math.min(from + size, all.size());
            List<CustomerReturn> slice = all.subList(from, to);
            boolean last = to >= all.size();
            return new PageResult<>(slice, all.size(), page, size, last);
        }

        @Override public Optional<CustomerReturn> loadById(Long id) { return Optional.empty(); }
        @Override public java.util.List<CustomerReturn> loadAllByOrderId(Long oid) { return List.of(); }
        @Override public PageResult<CustomerReturn> loadByPickpointIdAndStatus(Long p, CustomerReturnStatus s, int pg, int sz) {
            return new PageResult<>(List.of(), 0, pg, sz, true);
        }
        @Override public PageResult<CustomerReturn> loadAllPaged(int pg, int sz, String search) {
            return new PageResult<>(List.of(), 0, pg, sz, true);
        }
    }

    @Test
    @DisplayName("User with 3 returns, page 1 size 2 → 2 items, last=false")
    void page1_returnsFirstTwo() {
        var port = new FakeLoadCustomerReturnPort(
                List.of(fakeReturn(1L), fakeReturn(2L), fakeReturn(3L)));
        var svc  = new GetMyReturnsService(port);

        PageResult<CustomerReturn> result = svc.execute(10L, 1, 2);

        assertEquals(2, result.content().size());
        assertEquals(3, result.totalElements());
        assertFalse(result.last());
    }

    @Test
    @DisplayName("User with 3 returns, page 2 size 2 → 1 item, last=true")
    void page2_returnsLast() {
        var port = new FakeLoadCustomerReturnPort(
                List.of(fakeReturn(1L), fakeReturn(2L), fakeReturn(3L)));
        var svc  = new GetMyReturnsService(port);

        PageResult<CustomerReturn> result = svc.execute(10L, 2, 2);

        assertEquals(1, result.content().size());
        assertTrue(result.last());
    }

    @Test
    @DisplayName("User with no returns → empty page")
    void noReturns_emptyPage() {
        var port = new FakeLoadCustomerReturnPort(List.of());
        var svc  = new GetMyReturnsService(port);

        PageResult<CustomerReturn> result = svc.execute(10L, 1, 20);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        assertTrue(result.last());
    }
}
