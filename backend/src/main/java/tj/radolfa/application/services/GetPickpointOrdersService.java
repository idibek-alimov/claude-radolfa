package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.order.GetPickpointOrdersUseCase;
import tj.radolfa.application.ports.out.LoadPickpointOrdersPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.util.List;

@Service
public class GetPickpointOrdersService implements GetPickpointOrdersUseCase {

    private final LoadUserPort          loadUserPort;
    private final LoadPickpointOrdersPort loadPickpointOrdersPort;

    public GetPickpointOrdersService(LoadUserPort loadUserPort,
                                     LoadPickpointOrdersPort loadPickpointOrdersPort) {
        this.loadUserPort           = loadUserPort;
        this.loadPickpointOrdersPort = loadPickpointOrdersPort;
    }

    @Override
    public List<Order> execute(Long staffUserId) {
        var user = loadUserPort.loadById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + staffUserId));

        if (user.pickpointId() == null) {
            throw new IllegalStateException(
                    "PICKPOINT_STAFF user " + staffUserId + " has no pickpointId assigned");
        }

        return loadPickpointOrdersPort.loadByPickpointIdAndStatus(
                user.pickpointId(), OrderStatus.READY_FOR_PICKUP);
    }
}
