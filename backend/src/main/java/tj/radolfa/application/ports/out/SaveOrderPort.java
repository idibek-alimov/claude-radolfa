package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Order;

public interface SaveOrderPort {
    Order save(Order order);
}
