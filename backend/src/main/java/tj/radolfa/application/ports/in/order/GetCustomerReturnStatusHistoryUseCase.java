package tj.radolfa.application.ports.in.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.CustomerReturnStatusChange;

public interface GetCustomerReturnStatusHistoryUseCase {
    Page<CustomerReturnStatusChange> execute(Long returnId, Pageable pageable);
}
