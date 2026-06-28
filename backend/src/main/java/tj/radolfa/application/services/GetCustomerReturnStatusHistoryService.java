package tj.radolfa.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GetCustomerReturnStatusHistoryUseCase;
import tj.radolfa.application.ports.out.LoadCustomerReturnStatusChangePort;
import tj.radolfa.domain.model.CustomerReturnStatusChange;

@Service
@Transactional(readOnly = true)
public class GetCustomerReturnStatusHistoryService implements GetCustomerReturnStatusHistoryUseCase {

    private final LoadCustomerReturnStatusChangePort loadCustomerReturnStatusChangePort;

    public GetCustomerReturnStatusHistoryService(
            LoadCustomerReturnStatusChangePort loadCustomerReturnStatusChangePort) {
        this.loadCustomerReturnStatusChangePort = loadCustomerReturnStatusChangePort;
    }

    @Override
    public Page<CustomerReturnStatusChange> execute(Long returnId, Pageable pageable) {
        return loadCustomerReturnStatusChangePort.findByReturnId(returnId, pageable);
    }
}
