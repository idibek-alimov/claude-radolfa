package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GetAdminOrderDetailUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.Pickpoint;
import tj.radolfa.domain.model.User;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetAdminOrderDetailService implements GetAdminOrderDetailUseCase {

    private final LoadOrderPort     loadOrderPort;
    private final LoadPickpointPort loadPickpointPort;
    private final LoadUserPort      loadUserPort;

    public GetAdminOrderDetailService(LoadOrderPort loadOrderPort,
                                      LoadPickpointPort loadPickpointPort,
                                      LoadUserPort loadUserPort) {
        this.loadOrderPort     = loadOrderPort;
        this.loadPickpointPort = loadPickpointPort;
        this.loadUserPort      = loadUserPort;
    }

    @Override
    public Result execute(Long orderId) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        Optional<Pickpoint> pickpoint = order.pickpointId() != null
                ? loadPickpointPort.findById(order.pickpointId())
                : Optional.empty();

        User user = loadUserPort.loadById(order.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + order.userId()));

        String courierName = null;
        if (order.courierId() != null) {
            courierName = loadUserPort.loadById(order.courierId())
                    .map(User::name)
                    .orElse(null);
        }

        return new Result(order, pickpoint, user.phone().value(), user.name(), courierName);
    }
}
