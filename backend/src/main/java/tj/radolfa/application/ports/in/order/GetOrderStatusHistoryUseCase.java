package tj.radolfa.application.ports.in.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.OrderStatusChange;

public interface GetOrderStatusHistoryUseCase {
    Page<OrderStatusChange> execute(Long orderId, Pageable pageable);
}
