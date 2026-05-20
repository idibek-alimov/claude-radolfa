package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.PageResult;

public interface GetMyOrdersUseCase {
    PageResult<Order> execute(Long userId, int page, int size);
}
