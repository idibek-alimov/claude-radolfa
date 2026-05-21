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

class GetCustomerReturnsForOrderServiceTest {

    static CustomerReturn ret(Long id, Long orderId) {
        return new CustomerReturn(id, orderId, 5L, 99L,
                Instant.now(), null, List.of(),
                CustomerReturnStatus.RECEIVED,
                null, null, null, null, null, null);
    }

    static LoadCustomerReturnPort portWithReturns(List<CustomerReturn> returns) {
        return new LoadCustomerReturnPort() {
            @Override
            public Optional<CustomerReturn> loadById(Long id) { return Optional.empty(); }

            @Override
            public List<CustomerReturn> loadAllByOrderId(Long orderId) {
                return returns.stream().filter(r -> r.getOrderId().equals(orderId)).toList();
            }

            @Override
            public PageResult<CustomerReturn> loadByPickpointIdAndStatus(
                    Long pickpointId, CustomerReturnStatus status, int page, int size) {
                return new PageResult<>(List.of(), 0, page, size, true);
            }

            @Override
            public PageResult<CustomerReturn> loadAllPaged(int page, int size, String search) {
                return new PageResult<>(List.of(), 0, page, size, true);
            }
        };
    }

    @Test
    @DisplayName("Returns empty list when no returns exist for the given order")
    void noReturns_returnsEmptyList() {
        var port    = portWithReturns(List.of(ret(1L, 200L)));
        var service = new GetCustomerReturnsForOrderService(port);

        List<CustomerReturn> result = service.execute(100L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Returns only the returns belonging to the given orderId")
    void withReturns_returnsMatchingList() {
        var port    = portWithReturns(List.of(ret(1L, 100L), ret(2L, 200L)));
        var service = new GetCustomerReturnsForOrderService(port);

        List<CustomerReturn> result = service.execute(100L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(100L, result.get(0).getOrderId());
    }
}
