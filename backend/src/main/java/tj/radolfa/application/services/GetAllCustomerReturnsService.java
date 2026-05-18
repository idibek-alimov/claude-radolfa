package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GetAllCustomerReturnsUseCase;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.PageResult;

@Service
public class GetAllCustomerReturnsService implements GetAllCustomerReturnsUseCase {

    private final LoadCustomerReturnPort loadCustomerReturnPort;

    public GetAllCustomerReturnsService(LoadCustomerReturnPort loadCustomerReturnPort) {
        this.loadCustomerReturnPort = loadCustomerReturnPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CustomerReturn> execute(int page, int size) {
        return loadCustomerReturnPort.loadAllPaged(page, size);
    }
}
