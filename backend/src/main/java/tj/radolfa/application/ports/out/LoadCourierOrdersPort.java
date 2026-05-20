package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.util.List;

public interface LoadCourierOrdersPort {
    List<Order> loadByCourierIdAndStatuses(Long courierId, List<OrderStatus> statuses);

    PageResult<Order> loadByCourierIdAndStatusesPaged(Long courierId,
                                                       List<OrderStatus> statuses,
                                                       int page, int size);
}
