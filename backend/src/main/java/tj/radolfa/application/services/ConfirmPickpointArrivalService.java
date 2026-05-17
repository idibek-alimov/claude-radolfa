package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.ConfirmPickpointArrivalUseCase;
import tj.radolfa.application.ports.in.order.GenerateDeliveryCodeUseCase;
import tj.radolfa.application.ports.out.DeliveryEventPublisher;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.PickpointAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;

@Service
public class ConfirmPickpointArrivalService implements ConfirmPickpointArrivalUseCase {

    private final LoadOrderPort              loadOrderPort;
    private final SaveOrderPort              saveOrderPort;
    private final LoadUserPort               loadUserPort;
    private final GenerateDeliveryCodeUseCase generateDeliveryCodeUseCase;
    private final DeliveryEventPublisher     deliveryEventPublisher;

    public ConfirmPickpointArrivalService(LoadOrderPort loadOrderPort,
                                          SaveOrderPort saveOrderPort,
                                          LoadUserPort loadUserPort,
                                          GenerateDeliveryCodeUseCase generateDeliveryCodeUseCase,
                                          DeliveryEventPublisher deliveryEventPublisher) {
        this.loadOrderPort              = loadOrderPort;
        this.saveOrderPort              = saveOrderPort;
        this.loadUserPort               = loadUserPort;
        this.generateDeliveryCodeUseCase = generateDeliveryCodeUseCase;
        this.deliveryEventPublisher      = deliveryEventPublisher;
    }

    @Override
    @Transactional
    public void execute(Long orderId, Long staffUserId) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.status() != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Order is not SHIPPED: " + order.status());
        }
        if (order.deliveryType() != DeliveryType.PICKPOINT) {
            throw new IllegalStateException("Order is not a PICKPOINT delivery: " + order.deliveryType());
        }

        var staff = loadUserPort.loadById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + staffUserId));

        if (staff.pickpointId() == null || !staff.pickpointId().equals(order.pickpointId())) {
            throw new PickpointAccessDeniedException(
                    "Staff " + staffUserId + " is not assigned to pickpoint " + order.pickpointId());
        }

        Order updated = order.toBuilder()
                .status(OrderStatus.READY_FOR_PICKUP)
                .readyForPickupAt(Instant.now())
                .build();
        saveOrderPort.save(updated);

        generateDeliveryCodeUseCase.execute(orderId);
        deliveryEventPublisher.publishNewOrderAtPickpoint(order.pickpointId(), orderId);
    }
}
