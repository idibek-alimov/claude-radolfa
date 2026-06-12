package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GetAvailableOrdersUseCase;
import tj.radolfa.application.ports.out.LoadCourierOrdersPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.PageResult;

@Service
public class GetAvailableOrdersService implements GetAvailableOrdersUseCase {

    private final LoadCourierOrdersPort loadCourierOrdersPort;

    public GetAvailableOrdersService(LoadCourierOrdersPort loadCourierOrdersPort) {
        this.loadCourierOrdersPort = loadCourierOrdersPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Order> execute(int page, int size) {
        return loadCourierOrdersPort.loadAvailablePoolPaged(page, size);
    }
}
