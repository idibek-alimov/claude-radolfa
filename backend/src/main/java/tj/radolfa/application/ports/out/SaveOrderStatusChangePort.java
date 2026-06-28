package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.OrderStatusChange;

public interface SaveOrderStatusChangePort {
    OrderStatusChange append(OrderStatusChange change);
}
