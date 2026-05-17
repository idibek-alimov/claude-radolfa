package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.ExpireOrderUseCase;
import tj.radolfa.application.ports.in.payment.FailPaymentUseCase;
import tj.radolfa.application.ports.out.LoadPaymentPort;
import tj.radolfa.application.ports.out.SavePaymentPort;
import tj.radolfa.domain.model.Payment;
import tj.radolfa.domain.model.PaymentStatus;

@Service
public class FailPaymentService implements FailPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(FailPaymentService.class);

    private final LoadPaymentPort    loadPaymentPort;
    private final SavePaymentPort    savePaymentPort;
    private final ExpireOrderUseCase expireOrderUseCase;

    public FailPaymentService(LoadPaymentPort loadPaymentPort,
                              SavePaymentPort savePaymentPort,
                              ExpireOrderUseCase expireOrderUseCase) {
        this.loadPaymentPort    = loadPaymentPort;
        this.savePaymentPort    = savePaymentPort;
        this.expireOrderUseCase = expireOrderUseCase;
    }

    @Override
    @Transactional
    public void execute(String providerTransactionId) {
        Payment payment = loadPaymentPort.findByProviderTransactionId(providerTransactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment found for transaction: " + providerTransactionId));

        // Idempotency guard
        if (payment.status() == PaymentStatus.FAILED
                || payment.status() == PaymentStatus.CANCELLED) {
            log.info("[FailPayment] Already terminal ({}) — skipping. tx={}",
                    payment.status(), providerTransactionId);
            return;
        }
        if (payment.status() == PaymentStatus.COMPLETED) {
            log.warn("[FailPayment] Cannot fail an already COMPLETED payment. tx={}",
                    providerTransactionId);
            return;
        }

        savePaymentPort.save(payment.failed());

        // Cancel the order — restores stock and unlinks the cart (via CancelOrderService)
        expireOrderUseCase.execute(payment.orderId(), "Payment failed");

        log.info("[FailPayment] Payment failed, order cancelled. tx={} orderId={}",
                providerTransactionId, payment.orderId());
    }
}
