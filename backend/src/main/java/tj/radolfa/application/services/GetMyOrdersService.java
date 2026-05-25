package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.GetMyOrdersUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.PageResult;

@Service
public class GetMyOrdersService implements GetMyOrdersUseCase {

    private final LoadOrderPort loadOrderPort;

    public GetMyOrdersService(LoadOrderPort loadOrderPort) {
        this.loadOrderPort = loadOrderPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Order> execute(Long userId, int page, int size) {
        return loadOrderPort.loadByUserIdPaged(userId, page, size);
    }
}
