package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GetMyOrderByIdUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Order;

@Service
public class GetMyOrderByIdService implements GetMyOrderByIdUseCase {

    private final LoadOrderPort loadOrderPort;

    public GetMyOrderByIdService(LoadOrderPort loadOrderPort) {
        this.loadOrderPort = loadOrderPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Order execute(Long orderId, Long requestingUserId) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (!order.userId().equals(requestingUserId)) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        return order;
    }
}
