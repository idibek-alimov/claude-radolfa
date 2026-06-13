package tj.radolfa.application.ports.in;

import tj.radolfa.application.ports.in.order.MyOrderFilter;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.PageResult;

public interface GetMyOrdersUseCase {
    PageResult<Order> execute(Long userId, MyOrderFilter filter, int page, int size);
}
