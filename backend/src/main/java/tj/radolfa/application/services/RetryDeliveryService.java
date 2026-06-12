package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GenerateDeliveryCodeUseCase;
import tj.radolfa.application.ports.in.order.RetryDeliveryUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.CourierAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;

@Service
public class RetryDeliveryService implements RetryDeliveryUseCase {

    private final LoadOrderPort             loadOrderPort;
    private final SaveOrderPort             saveOrderPort;
    private final OrderNotificationService  orderNotificationService;
    private final GenerateDeliveryCodeUseCase generateDeliveryCodeUseCase;

    public RetryDeliveryService(LoadOrderPort loadOrderPort,
                                SaveOrderPort saveOrderPort,
                                OrderNotificationService orderNotificationService,
                                GenerateDeliveryCodeUseCase generateDeliveryCodeUseCase) {
        this.loadOrderPort              = loadOrderPort;
        this.saveOrderPort              = saveOrderPort;
        this.orderNotificationService   = orderNotificationService;
        this.generateDeliveryCodeUseCase = generateDeliveryCodeUseCase;
    }

    @Override
    @Transactional
    public void execute(Long orderId, Long courierId) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.status() != OrderStatus.DELIVERY_ATTEMPTED) {
            throw new IllegalStateException(
                    "Order must be DELIVERY_ATTEMPTED to retry delivery, current status: " + order.status());
        }

        if (order.courierId() == null || !order.courierId().equals(courierId)) {
            throw new CourierAccessDeniedException(
                    "Courier " + courierId + " is not assigned to order " + orderId);
        }

        Order updated = order.toBuilder()
                .status(OrderStatus.OUT_FOR_DELIVERY)
                .outForDeliveryAt(Instant.now())
                .build();

        saveOrderPort.save(updated);
        generateDeliveryCodeUseCase.execute(orderId);
        orderNotificationService.notify(updated);
    }
}
