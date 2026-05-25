package tj.radolfa.application.ports.out;

import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.util.Collection;
import java.util.List;

public interface LoadPickpointOrdersPort {

    PageResult<Order> loadByPickpointIdAndStatuses(Long pickpointId, Collection<OrderStatus> statuses, Pageable pageable);

    default List<Order> loadByPickpointIdAndStatuses(Long pickpointId, Collection<OrderStatus> statuses) {
        return loadByPickpointIdAndStatuses(
                pickpointId, statuses,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .content();
    }

    default List<Order> loadByPickpointIdAndStatus(Long pickpointId, OrderStatus status) {
        return loadByPickpointIdAndStatuses(pickpointId, List.of(status));
    }
}
