package tj.radolfa.infrastructure.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.PaymentPort;
import tj.radolfa.domain.model.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Dev/test stub for {@link PaymentPort}.
 *
 * <p>Always succeeds immediately. Logs the call so developers can see what
 * a real gateway would receive. Replace with a real adapter in Phase 8.
 */
@Component
@Profile("dev | test")
public class PaymentPortStub implements PaymentPort {

    private static final Logger log = LoggerFactory.getLogger(PaymentPortStub.class);

    @Override
    public PaymentIntent initiate(Money amount,
                                  String currency,
                                  String externalOrderId,
                                  String customerId) {
        String txId = "STUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[PAYMENT STUB] initiate: orderId={} customer={} amount={} {} → txId={}",
                externalOrderId, customerId, amount.amount(), currency, txId);
        return new PaymentIntent(
                txId,
                "/stub/payment/success?tx=" + txId,
                Instant.now().plusSeconds(3600)
        );
    }

    @Override
    public RefundResult refund(String providerTransactionId, Money amount) {
        String refundId = "REFUND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[PAYMENT STUB] refund: tx={} amount={} → refundId={}",
                providerTransactionId, amount.amount(), refundId);
        return new RefundResult(refundId, true, "Stub refund processed successfully.");
    }
}
