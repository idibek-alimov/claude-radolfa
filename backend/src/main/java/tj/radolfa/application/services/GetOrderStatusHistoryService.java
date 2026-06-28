package tj.radolfa.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GetOrderStatusHistoryUseCase;
import tj.radolfa.application.ports.out.LoadOrderStatusChangePort;
import tj.radolfa.domain.model.OrderStatusChange;

@Service
@Transactional(readOnly = true)
public class GetOrderStatusHistoryService implements GetOrderStatusHistoryUseCase {

    private final LoadOrderStatusChangePort loadOrderStatusChangePort;

    public GetOrderStatusHistoryService(LoadOrderStatusChangePort loadOrderStatusChangePort) {
        this.loadOrderStatusChangePort = loadOrderStatusChangePort;
    }

    @Override
    public Page<OrderStatusChange> execute(Long orderId, Pageable pageable) {
        return loadOrderStatusChangePort.findByOrderId(orderId, pageable);
    }
}
