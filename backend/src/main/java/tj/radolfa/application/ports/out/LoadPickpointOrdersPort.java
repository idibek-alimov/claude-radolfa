package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.util.Collection;
import java.util.List;

public interface LoadPickpointOrdersPort {

    List<Order> loadByPickpointIdAndStatuses(Long pickpointId, Collection<OrderStatus> statuses);

    default List<Order> loadByPickpointIdAndStatus(Long pickpointId, OrderStatus status) {
        return loadByPickpointIdAndStatuses(pickpointId, List.of(status));
    }
}
