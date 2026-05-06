package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

public interface LoadAdminOrdersPort {

    record OrderRow(Order order, String userPhone, String userName) {}

    PageResult<OrderRow> search(String search,
                                OrderStatus statusFilter,
                                String sortBy,
                                String sortDir,
                                int page,
                                int size);
}
