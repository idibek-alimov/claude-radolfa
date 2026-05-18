package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.PageResult;

public interface GetAllCustomerReturnsUseCase {
    PageResult<CustomerReturn> execute(int page, int size, String search);
}
