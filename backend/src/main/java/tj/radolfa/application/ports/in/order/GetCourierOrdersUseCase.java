package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.Order;

import java.util.List;

public interface GetCourierOrdersUseCase {
    List<Order> execute(Long courierId);
}
