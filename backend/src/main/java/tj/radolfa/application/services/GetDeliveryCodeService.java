package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.order.GetDeliveryCodeUseCase;
import tj.radolfa.application.ports.out.LoadDeliveryCodePort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.domain.exception.CourierAccessDeniedException;
import tj.radolfa.domain.exception.DeliveryCodeNotFoundException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.util.Set;

@Service
public class GetDeliveryCodeService implements GetDeliveryCodeUseCase {

    private static final Set<OrderStatus> CODE_VISIBLE_STATUSES = Set.of(
            OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.READY_FOR_PICKUP);

    private final LoadOrderPort        loadOrderPort;
    private final LoadDeliveryCodePort loadDeliveryCodePort;

    public GetDeliveryCodeService(LoadOrderPort loadOrderPort,
                                  LoadDeliveryCodePort loadDeliveryCodePort) {
        this.loadOrderPort        = loadOrderPort;
        this.loadDeliveryCodePort = loadDeliveryCodePort;
    }

    @Override
    public Result execute(Long orderId, Long requestingUserId) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.userId().equals(requestingUserId)) {
            throw new CourierAccessDeniedException("Access denied to order " + orderId);
        }

        if (!CODE_VISIBLE_STATUSES.contains(order.status())) {
            throw new IllegalArgumentException(
                    "No delivery code available for order in status: " + order.status());
        }

        var code = loadDeliveryCodePort.loadActiveByOrderId(orderId)
                .orElseThrow(() -> new DeliveryCodeNotFoundException("No active code found for order " + orderId));

        return new Result(code.getCode(), code.getExpiresAt());
    }
}
