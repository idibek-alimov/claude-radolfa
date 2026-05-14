package tj.radolfa.application.ports.in.order;

import tj.radolfa.application.ports.out.LoadAdminOrdersPort;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

public interface ListAdminOrdersUseCase {

    PageResult<LoadAdminOrdersPort.OrderRow> execute(String search,
                                                     OrderStatus status,
                                                     String sortBy,
                                                     String sortDir,
                                                     int page,
                                                     int size);
}
