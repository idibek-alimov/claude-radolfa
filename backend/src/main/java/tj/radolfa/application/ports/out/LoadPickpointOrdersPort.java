package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.util.List;

public interface LoadPickpointOrdersPort {
    List<Order> loadByPickpointIdAndStatus(Long pickpointId, OrderStatus status);
}
