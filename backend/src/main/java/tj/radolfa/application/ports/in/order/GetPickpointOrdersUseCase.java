package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.Order;

import java.util.List;

public interface GetPickpointOrdersUseCase {
    List<Order> execute(Long staffUserId);
}
