package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.domain.model.CustomerReturn;

public interface GetWarehouseCustomerReturnByIdUseCase {
    CustomerReturn execute(Long returnId);
}
