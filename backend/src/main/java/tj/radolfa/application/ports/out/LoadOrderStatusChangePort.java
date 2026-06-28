package tj.radolfa.application.ports.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.OrderStatusChange;

public interface LoadOrderStatusChangePort {
    Page<OrderStatusChange> findByOrderId(Long orderId, Pageable pageable);
}
