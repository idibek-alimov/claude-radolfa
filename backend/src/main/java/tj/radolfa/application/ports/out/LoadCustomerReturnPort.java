package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface LoadCustomerReturnPort {
    Optional<CustomerReturn> loadById(Long id);
    List<CustomerReturn> loadAllByOrderId(Long orderId);
    PageResult<CustomerReturn> loadByPickpointIdAndStatus(Long pickpointId, CustomerReturnStatus status, int page, int size);
    PageResult<CustomerReturn> loadAllPaged(int page, int size, String search);
    default PageResult<CustomerReturn> loadByUserId(Long userId, int page, int size) {
        return new PageResult<>(java.util.List.of(), 0, page, size, true);
    }
}
