package tj.radolfa.application.services;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.order.GetPickpointOrdersUseCase;
import tj.radolfa.application.ports.out.LoadPickpointOrdersPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.util.List;

@Service
public class GetPickpointOrdersService implements GetPickpointOrdersUseCase {

    private final LoadUserPort           loadUserPort;
    private final LoadPickpointOrdersPort loadPickpointOrdersPort;

    public GetPickpointOrdersService(LoadUserPort loadUserPort,
                                     LoadPickpointOrdersPort loadPickpointOrdersPort) {
        this.loadUserPort            = loadUserPort;
        this.loadPickpointOrdersPort = loadPickpointOrdersPort;
    }

    @Override
    public PageResult<Order> execute(Long staffUserId, List<OrderStatus> statuses, int page, int size) {
        var user = loadUserPort.loadById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + staffUserId));

        if (user.pickpointId() == null) {
            throw new IllegalStateException(
                    "PICKPOINT_STAFF user " + staffUserId + " has no pickpointId assigned");
        }

        List<OrderStatus> effectiveStatuses = (statuses == null || statuses.isEmpty())
                ? List.of(OrderStatus.READY_FOR_PICKUP)
                : statuses;

        // page param is 1-based; Spring Pageable is 0-based
        var pageable = PageRequest.of(Math.max(0, page - 1), size);

        return loadPickpointOrdersPort.loadByPickpointIdAndStatuses(
                user.pickpointId(), effectiveStatuses, pageable);
    }
}
