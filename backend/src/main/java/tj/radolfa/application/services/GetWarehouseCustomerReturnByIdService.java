package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.GetWarehouseCustomerReturnByIdUseCase;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnStatus;

@Service
@Transactional(readOnly = true)
public class GetWarehouseCustomerReturnByIdService implements GetWarehouseCustomerReturnByIdUseCase {

    private final LoadCustomerReturnPort loadCustomerReturnPort;

    public GetWarehouseCustomerReturnByIdService(LoadCustomerReturnPort loadCustomerReturnPort) {
        this.loadCustomerReturnPort = loadCustomerReturnPort;
    }

    @Override
    public CustomerReturn execute(Long returnId) {
        CustomerReturn ret = loadCustomerReturnPort.loadById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer return not found: " + returnId));
        if (ret.getStatus() != CustomerReturnStatus.SENT_TO_WAREHOUSE) {
            throw new ResourceNotFoundException("Customer return not found: " + returnId);
        }
        return ret;
    }
}
