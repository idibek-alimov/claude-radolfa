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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetWarehouseCustomerReturnsServiceTest {

    static CustomerReturn ret(Long id, CustomerReturnStatus status) {
        return new CustomerReturn(id, 100L, 5L, 99L,
                Instant.now(), null, List.of(),
                status,
                null, null, null, null, null, null);
    }

    static LoadCustomerReturnPort portWithReturns(List<CustomerReturn> all) {
        return new LoadCustomerReturnPort() {
            @Override
            public Optional<CustomerReturn> loadById(Long id) { return Optional.empty(); }

            @Override
            public java.util.List<CustomerReturn> loadAllByOrderId(Long orderId) { return List.of(); }

            @Override
            public PageResult<CustomerReturn> loadByPickpointIdAndStatus(
                    Long pickpointId, CustomerReturnStatus status, int page, int size) {
                return new PageResult<>(List.of(), 0, page, size, true);
            }

            @Override
            public PageResult<CustomerReturn> loadAllPaged(int page, int size, String search) {
                return new PageResult<>(List.of(), 0, page, size, true);
            }

            @Override
            public PageResult<CustomerReturn> loadByStatus(CustomerReturnStatus status, int page, int size) {
                List<CustomerReturn> filtered = all.stream()
                        .filter(r -> r.getStatus() == status)
                        .toList();
                boolean last = page * size >= filtered.size();
                int from = Math.min((page - 1) * size, filtered.size());
                int to   = Math.min(page * size, filtered.size());
                return new PageResult<>(filtered.subList(from, to), filtered.size(), page, size, last);
            }
        };
    }

    @Test
    @DisplayName("Returns only SENT_TO_WAREHOUSE returns — other statuses excluded")
    void filtersToSentToWarehouseOnly() {
        var port = portWithReturns(List.of(
                ret(1L, CustomerReturnStatus.SENT_TO_WAREHOUSE),
                ret(2L, CustomerReturnStatus.RECEIVED),
                ret(3L, CustomerReturnStatus.SENT_TO_WAREHOUSE)
        ));
        var service = new GetWarehouseCustomerReturnsService(port);

        PageResult<CustomerReturn> result = service.execute(1, 20);

        assertEquals(2, result.totalElements());
        assertTrue(result.content().stream()
                .allMatch(r -> r.getStatus() == CustomerReturnStatus.SENT_TO_WAREHOUSE));
    }

    @Test
    @DisplayName("Returns empty page when no SENT_TO_WAREHOUSE returns exist")
    void noMatchingReturns_returnsEmptyPage() {
        var port    = portWithReturns(List.of(ret(1L, CustomerReturnStatus.RECEIVED)));
        var service = new GetWarehouseCustomerReturnsService(port);

        PageResult<CustomerReturn> result = service.execute(1, 20);

        assertEquals(0, result.totalElements());
        assertTrue(result.content().isEmpty());
    }

    @Test
    @DisplayName("Pagination: page 1 with size 1 returns first item only")
    void pagination_returnsCorrectSlice() {
        var port = portWithReturns(List.of(
                ret(1L, CustomerReturnStatus.SENT_TO_WAREHOUSE),
                ret(2L, CustomerReturnStatus.SENT_TO_WAREHOUSE)
        ));
        var service = new GetWarehouseCustomerReturnsService(port);

        PageResult<CustomerReturn> page1 = service.execute(1, 1);
        PageResult<CustomerReturn> page2 = service.execute(2, 1);

        assertEquals(2, page1.totalElements());
        assertEquals(1, page1.content().size());
        assertEquals(1L, page1.content().get(0).getId());
        assertEquals(1, page2.content().size());
        assertEquals(2L, page2.content().get(0).getId());
    }
}
