// TODO: Replace with real payment gateway implementation.
// Implement ProcessRefundPort in a new class (e.g. PaymeRefundAdapter, ClickRefundAdapter).
// Annotate it @Component @Profile("refund-gateway") and set SPRING_PROFILES_ACTIVE accordingly.
// Remove @Primary from this stub once the real adapter is active.

package tj.radolfa.infrastructure.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.ProcessRefundPort;
import tj.radolfa.domain.model.Money;

import java.util.UUID;

@Component
@Primary
public class ProcessRefundPortStub implements ProcessRefundPort {

    private static final Logger log = LoggerFactory.getLogger(ProcessRefundPortStub.class);

    @Override
    public RefundResult process(Long orderId, String externalPaymentId, Money amount) {
        log.warn("[REFUND STUB] Would refund {} for order {} (payment {}). " +
                 "Returning synthetic success. Replace this stub with a real gateway adapter.",
                 amount, orderId, externalPaymentId);
        return new RefundResult(true, "STUB-" + UUID.randomUUID(), null);
    }
}
