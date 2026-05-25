package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GetMyReturnsUseCase;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.PageResult;

@Service
public class GetMyReturnsService implements GetMyReturnsUseCase {

    private final LoadCustomerReturnPort loadCustomerReturnPort;

    public GetMyReturnsService(LoadCustomerReturnPort loadCustomerReturnPort) {
        this.loadCustomerReturnPort = loadCustomerReturnPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CustomerReturn> execute(Long userId, int page, int size) {
        return loadCustomerReturnPort.loadByUserId(userId, page, size);
    }
}
