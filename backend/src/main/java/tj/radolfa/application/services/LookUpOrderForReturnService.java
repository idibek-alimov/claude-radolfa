package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.LookUpOrderForReturnUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.exception.OrderNotAtPickpointException;
import tj.radolfa.domain.exception.OrderNotDeliveredException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

@Service
public class LookUpOrderForReturnService implements LookUpOrderForReturnUseCase {

    private final LoadOrderPort loadOrderPort;
    private final LoadUserPort  loadUserPort;

    public LookUpOrderForReturnService(LoadOrderPort loadOrderPort, LoadUserPort loadUserPort) {
        this.loadOrderPort = loadOrderPort;
        this.loadUserPort  = loadUserPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Order execute(Long orderId, Long staffUserId) {
        var order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.status() != OrderStatus.DELIVERED) {
            throw new OrderNotDeliveredException(
                    "Order " + orderId + " is not DELIVERED: " + order.status());
        }

        var staff = loadUserPort.loadById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + staffUserId));

        if (order.deliveryType() != DeliveryType.PICKPOINT
                || staff.pickpointId() == null
                || !staff.pickpointId().equals(order.pickpointId())) {
            throw new OrderNotAtPickpointException(
                    "Order " + orderId + " is not a PICKPOINT order at this staff's pickpoint");
        }

        return order;
    }
}
