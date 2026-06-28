package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.ClaimOrderUseCase;
import tj.radolfa.application.ports.in.order.GenerateDeliveryCodeUseCase;
import tj.radolfa.application.ports.out.ClaimOrderPort;
import tj.radolfa.domain.exception.OrderAlreadyClaimedException;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;

@Service
public class ClaimOrderService implements ClaimOrderUseCase {

    private final ClaimOrderPort              claimOrderPort;
    private final GenerateDeliveryCodeUseCase generateDeliveryCodeUseCase;
    private final OrderStatusChangeRecorder   orderStatusChangeRecorder;

    public ClaimOrderService(ClaimOrderPort claimOrderPort,
                             GenerateDeliveryCodeUseCase generateDeliveryCodeUseCase,
                             OrderStatusChangeRecorder orderStatusChangeRecorder) {
        this.claimOrderPort              = claimOrderPort;
        this.generateDeliveryCodeUseCase  = generateDeliveryCodeUseCase;
        this.orderStatusChangeRecorder   = orderStatusChangeRecorder;
    }

    @Override
    @Transactional
    public void execute(Command cmd) {
        boolean claimed = claimOrderPort.claimIfAvailable(cmd.orderId(), cmd.courierId(), Instant.now());
        if (!claimed) {
            throw new OrderAlreadyClaimedException(
                    "Order " + cmd.orderId() + " is no longer available for claiming.");
        }
        // status_from is implicitly PICKED — the WHERE clause in claimIfAvailable guards this.
        orderStatusChangeRecorder.record(cmd.orderId(), OrderStatus.PICKED, OrderStatus.CLAIMED,
                cmd.courierId(), null);
        // Delivery code SMS fires at claim time for HOME orders.
        // GenerateDeliveryCodeService guard is updated to also accept CLAIMED status.
        generateDeliveryCodeUseCase.execute(cmd.orderId());
    }
}
