package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.PageResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetWarehouseCustomerReturnByIdServiceTest {

    static CustomerReturn ret(Long id, CustomerReturnStatus status) {
        return new CustomerReturn(id, 100L, 5L, 99L,
                Instant.now(), null, List.of(),
                status,
                status == CustomerReturnStatus.SENT_TO_WAREHOUSE ? Instant.now() : null,
                null, null, null, null, null);
    }

    static LoadCustomerReturnPort portWith(CustomerReturn... returns) {
        return new LoadCustomerReturnPort() {
            @Override
            public Optional<CustomerReturn> loadById(Long id) {
                return List.of(returns).stream().filter(r -> r.getId().equals(id)).findFirst();
            }
            @Override
            public List<CustomerReturn> loadAllByOrderId(Long orderId) { return List.of(); }
            @Override
            public PageResult<CustomerReturn> loadByPickpointIdAndStatus(Long pickpointId, CustomerReturnStatus status, int page, int size) {
                return new PageResult<>(List.of(), 0, page, size, true);
            }
            @Override
            public PageResult<CustomerReturn> loadAllPaged(int page, int size, String search) {
                return new PageResult<>(List.of(), 0, page, size, true);
            }
        };
    }

    @Test
    @DisplayName("Returns the return when it exists and status is SENT_TO_WAREHOUSE")
    void found() {
        var port = portWith(ret(1L, CustomerReturnStatus.SENT_TO_WAREHOUSE));
        var service = new GetWarehouseCustomerReturnByIdService(port);

        CustomerReturn result = service.execute(1L);

        assertEquals(1L, result.getId());
        assertEquals(CustomerReturnStatus.SENT_TO_WAREHOUSE, result.getStatus());
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when return does not exist")
    void notFound() {
        var port = portWith();
        var service = new GetWarehouseCustomerReturnByIdService(port);

        assertThrows(ResourceNotFoundException.class, () -> service.execute(99L));
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when return exists but status is not SENT_TO_WAREHOUSE")
    void wrongStatus() {
        var port = portWith(ret(2L, CustomerReturnStatus.RECEIVED));
        var service = new GetWarehouseCustomerReturnByIdService(port);

        assertThrows(ResourceNotFoundException.class, () -> service.execute(2L));
    }
}
