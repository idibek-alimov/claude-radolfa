package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Order;
import java.util.List;

public interface GetMyOrdersUseCase {
    List<Order> execute(Long userId);
}
