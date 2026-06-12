package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.ClaimOrderUseCase;
import tj.radolfa.application.ports.in.order.GenerateDeliveryCodeUseCase;
import tj.radolfa.application.ports.out.ClaimOrderPort;
import tj.radolfa.domain.exception.OrderAlreadyClaimedException;

import java.time.Instant;

@Service
public class ClaimOrderService implements ClaimOrderUseCase {

    private final ClaimOrderPort             claimOrderPort;
    private final GenerateDeliveryCodeUseCase generateDeliveryCodeUseCase;

    public ClaimOrderService(ClaimOrderPort claimOrderPort,
                             GenerateDeliveryCodeUseCase generateDeliveryCodeUseCase) {
        this.claimOrderPort             = claimOrderPort;
        this.generateDeliveryCodeUseCase = generateDeliveryCodeUseCase;
    }

    @Override
    @Transactional
    public void execute(Command cmd) {
        boolean claimed = claimOrderPort.claimIfAvailable(cmd.orderId(), cmd.courierId(), Instant.now());
        if (!claimed) {
            throw new OrderAlreadyClaimedException(
                    "Order " + cmd.orderId() + " is no longer available for claiming.");
        }
        // Delivery code SMS fires at claim time for HOME orders.
        // GenerateDeliveryCodeService guard is updated to also accept CLAIMED status.
        generateDeliveryCodeUseCase.execute(cmd.orderId());
    }
}
