package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.GetWarehouseCustomerReturnsUseCase;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.PageResult;

@Service
@Transactional(readOnly = true)
public class GetWarehouseCustomerReturnsService implements GetWarehouseCustomerReturnsUseCase {

    private final LoadCustomerReturnPort loadCustomerReturnPort;

    public GetWarehouseCustomerReturnsService(LoadCustomerReturnPort loadCustomerReturnPort) {
        this.loadCustomerReturnPort = loadCustomerReturnPort;
    }

    @Override
    public PageResult<CustomerReturn> execute(int page, int size) {
        return loadCustomerReturnPort.loadByStatus(CustomerReturnStatus.SENT_TO_WAREHOUSE, page, size);
    }
}
