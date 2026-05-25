package tj.radolfa.application.ports.in.order;

import tj.radolfa.application.ports.out.LoadAdminOrdersPort;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.util.List;

public interface ListAdminOrdersUseCase {

    PageResult<LoadAdminOrdersPort.OrderRow> execute(String search,
                                                     List<OrderStatus> statuses,
                                                     String sortBy,
                                                     String sortDir,
                                                     int page,
                                                     int size);
}
