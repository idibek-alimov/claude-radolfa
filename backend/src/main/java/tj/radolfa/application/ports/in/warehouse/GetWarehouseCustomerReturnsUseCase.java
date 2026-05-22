package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.PageResult;

public interface GetWarehouseCustomerReturnsUseCase {
    PageResult<CustomerReturn> execute(int page, int size);
}
