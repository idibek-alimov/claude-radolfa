package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.PageResult;

public interface GetPickpointCustomerReturnsUseCase {
    PageResult<CustomerReturn> execute(Long staffUserId, CustomerReturnStatus status, int page, int size);
}
