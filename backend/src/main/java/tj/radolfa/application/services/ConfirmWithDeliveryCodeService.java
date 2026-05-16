package tj.radolfa.application.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.ConfirmWithDeliveryCodeUseCase;
import tj.radolfa.application.ports.out.LoadDeliveryCodePort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.SaveDeliveryCodePort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.DeliveryCodeAlreadyUsedException;
import tj.radolfa.domain.exception.DeliveryCodeAttemptsExhaustedException;
import tj.radolfa.domain.exception.DeliveryCodeExpiredException;
import tj.radolfa.domain.exception.DeliveryCodeMismatchException;
import tj.radolfa.domain.exception.DeliveryCodeNotFoundException;
import tj.radolfa.domain.model.DeliveryCode;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;
import java.util.Set;

@Service
public class ConfirmWithDeliveryCodeService implements ConfirmWithDeliveryCodeUseCase {

    private static final Set<OrderStatus> VALID_PRECONDITIONS = Set.of(
            OrderStatus.SHIPPED,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.READY_FOR_PICKUP
    );

    private final LoadDeliveryCodePort   loadDeliveryCodePort;
    private final SaveDeliveryCodePort   saveDeliveryCodePort;
    private final LoadOrderPort          loadOrderPort;
    private final SaveOrderPort          saveOrderPort;
    private final OrderNotificationService orderNotificationService;
    private final int                    maxAttempts;

    public ConfirmWithDeliveryCodeService(LoadDeliveryCodePort loadDeliveryCodePort,
                                          SaveDeliveryCodePort saveDeliveryCodePort,
                                          LoadOrderPort loadOrderPort,
                                          SaveOrderPort saveOrderPort,
                                          OrderNotificationService orderNotificationService,
                                          @Value("${radolfa.delivery.code-max-attempts:5}") int maxAttempts) {
        this.loadDeliveryCodePort   = loadDeliveryCodePort;
        this.saveDeliveryCodePort   = saveDeliveryCodePort;
        this.loadOrderPort          = loadOrderPort;
        this.saveOrderPort          = saveOrderPort;
        this.orderNotificationService = orderNotificationService;
        this.maxAttempts            = maxAttempts;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        DeliveryCode code = loadDeliveryCodePort.loadActiveByOrderId(command.orderId())
                .orElseThrow(() -> new DeliveryCodeNotFoundException(
                        "No active delivery code for order: " + command.orderId()));

        if (code.isExpired()) {
            throw new DeliveryCodeExpiredException("Delivery code has expired for order: " + command.orderId());
        }
        if (code.isUsed()) {
            throw new DeliveryCodeAlreadyUsedException("Delivery code already used for order: " + command.orderId());
        }
        if (code.getAttemptCount() >= maxAttempts) {
            throw new DeliveryCodeAttemptsExhaustedException(
                    "Maximum verification attempts reached for order: " + command.orderId());
        }

        if (!code.getCode().equals(command.enteredCode())) {
            code.incrementAttempts();
            saveDeliveryCodePort.save(code);
            int remaining = maxAttempts - code.getAttemptCount();
            throw new DeliveryCodeMismatchException("Incorrect code. " + remaining + " attempts remaining.");
        }

        code.markUsed();
        saveDeliveryCodePort.save(code);

        Order order = loadOrderPort.loadById(command.orderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + command.orderId()));

        if (!VALID_PRECONDITIONS.contains(order.status())) {
            throw new IllegalStateException(
                    "Cannot confirm delivery for order in status: " + order.status());
        }

        Order delivered = order.toBuilder()
                .status(OrderStatus.DELIVERED)
                .deliveredAt(Instant.now())
                .build();
        saveOrderPort.save(delivered);
        orderNotificationService.notify(delivered);
    }
}
