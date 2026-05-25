package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.CustomerReturn;

import java.util.List;

public interface GetCustomerReturnsForOrderUseCase {
    List<CustomerReturn> execute(Long orderId);
}
