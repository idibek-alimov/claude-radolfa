package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GetCourierOrdersUseCase;
import tj.radolfa.application.ports.out.LoadCourierOrdersPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.util.List;

@Service
public class GetCourierOrdersService implements GetCourierOrdersUseCase {

    private final LoadCourierOrdersPort loadCourierOrdersPort;

    public GetCourierOrdersService(LoadCourierOrdersPort loadCourierOrdersPort) {
        this.loadCourierOrdersPort = loadCourierOrdersPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Order> execute(Long courierId, List<OrderStatus> statuses, int page, int size) {
        return loadCourierOrdersPort.loadByCourierIdAndStatusesPaged(courierId, statuses, page, size);
    }
}
