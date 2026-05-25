package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.util.List;

public interface GetCourierOrdersUseCase {
    PageResult<Order> execute(Long courierId, List<OrderStatus> statuses, int page, int size);
}
