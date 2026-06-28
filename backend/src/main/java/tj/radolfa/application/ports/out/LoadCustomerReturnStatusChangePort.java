package tj.radolfa.application.ports.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.CustomerReturnStatusChange;

public interface LoadCustomerReturnStatusChangePort {
    Page<CustomerReturnStatusChange> findByReturnId(Long returnId, Pageable pageable);
}
