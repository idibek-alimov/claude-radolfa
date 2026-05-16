package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import tj.radolfa.application.ports.in.order.GenerateDeliveryCodeUseCase;
import tj.radolfa.application.ports.in.order.RedirectToPickpointUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

@Service
public class RedirectToPickpointService implements RedirectToPickpointUseCase {

    private final LoadOrderPort              loadOrderPort;
    private final SaveOrderPort              saveOrderPort;
    private final LoadPickpointPort          loadPickpointPort;
    private final GenerateDeliveryCodeUseCase generateDeliveryCodeUseCase;
    private final OrderNotificationService   orderNotificationService;

    public RedirectToPickpointService(LoadOrderPort loadOrderPort,
                                      SaveOrderPort saveOrderPort,
                                      LoadPickpointPort loadPickpointPort,
                                      GenerateDeliveryCodeUseCase generateDeliveryCodeUseCase,
                                      OrderNotificationService orderNotificationService) {
        this.loadOrderPort              = loadOrderPort;
        this.saveOrderPort              = saveOrderPort;
        this.loadPickpointPort          = loadPickpointPort;
        this.generateDeliveryCodeUseCase = generateDeliveryCodeUseCase;
        this.orderNotificationService   = orderNotificationService;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        Order order = loadOrderPort.loadById(command.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + command.orderId()));

        if (order.status() != OrderStatus.DELIVERY_ATTEMPTED) {
            throw new IllegalStateException(
                    "Order " + command.orderId() + " must be DELIVERY_ATTEMPTED to redirect; was " + order.status());
        }
        if (order.deliveryType() != DeliveryType.HOME) {
            throw new IllegalStateException(
                    "Order " + command.orderId() + " is not a HOME delivery");
        }

        loadPickpointPort.findById(command.pickpointId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pickpoint not found: " + command.pickpointId()));

        Order updated = order.toBuilder()
                .status(OrderStatus.READY_FOR_PICKUP)
                .deliveryType(DeliveryType.PICKPOINT)
                .pickpointId(command.pickpointId())
                .courierId(null)
                .shippedAt(null)
                .outForDeliveryAt(null)
                .deliveryAttemptedAt(null)
                .readyForPickupAt(Instant.now())
                .build();

        saveOrderPort.save(updated);

        // Must save first (status = READY_FOR_PICKUP) before generating code — the
        // code service asserts order is in SHIPPED or READY_FOR_PICKUP before issuing.
        generateDeliveryCodeUseCase.execute(command.orderId());

        orderNotificationService.notify(updated);
    }
}
