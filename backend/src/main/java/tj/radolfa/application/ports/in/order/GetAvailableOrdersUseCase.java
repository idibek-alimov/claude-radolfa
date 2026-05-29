package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.PageResult;

public interface GetAvailableOrdersUseCase {
    PageResult<Order> execute(int page, int size);
}
