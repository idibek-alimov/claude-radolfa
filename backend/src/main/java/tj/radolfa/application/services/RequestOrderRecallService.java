package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.RequestOrderRecallUseCase;
import tj.radolfa.application.ports.out.DeliveryEventPublisher;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.OrderRecallNotAllowedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;
import java.util.Set;

@Service
public class RequestOrderRecallService implements RequestOrderRecallUseCase {

    private static final Set<OrderStatus> RECALLABLE = Set.of(
            OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.READY_FOR_PICKUP);

    private static final Set<OrderStatus> COURIER_STATUSES = Set.of(
            OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERY_ATTEMPTED);

    private final LoadOrderPort         loadOrderPort;
    private final SaveOrderPort         saveOrderPort;
    private final DeliveryEventPublisher deliveryEventPublisher;

    public RequestOrderRecallService(LoadOrderPort loadOrderPort,
                                      SaveOrderPort saveOrderPort,
                                      DeliveryEventPublisher deliveryEventPublisher) {
        this.loadOrderPort          = loadOrderPort;
        this.saveOrderPort          = saveOrderPort;
        this.deliveryEventPublisher = deliveryEventPublisher;
    }

    @Override
    @Transactional
    public void execute(Long orderId, Long adminUserId, String reason) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!RECALLABLE.contains(order.status())) {
            throw new OrderRecallNotAllowedException(order.status());
        }

        Order recalled = order.toBuilder()
                .status(OrderStatus.RECALL_REQUESTED)
                .recallRequestedAt(Instant.now())
                .recallRequestedByUserId(adminUserId)
                .recallReason(reason)
                .build();
        saveOrderPort.save(recalled);

        if (COURIER_STATUSES.contains(order.status()) && order.courierId() != null) {
            deliveryEventPublisher.publishOrderRecallToCourier(order.courierId(), orderId);
        } else if (order.status() == OrderStatus.READY_FOR_PICKUP && order.pickpointId() != null) {
            deliveryEventPublisher.publishOrderRecallToPickpoint(order.pickpointId(), orderId);
        }
    }
}
