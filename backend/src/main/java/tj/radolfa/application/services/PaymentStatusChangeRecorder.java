package tj.radolfa.application.services;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.SavePaymentStatusChangePort;
import tj.radolfa.domain.model.PaymentStatus;
import tj.radolfa.domain.model.PaymentStatusChange;

import java.time.Instant;

/**
 * Centralised helper for appending a {@code payment_status_changes} row.
 *
 * <p>Must be called inside an existing {@code @Transactional} boundary so the ledger
 * row and the status change are committed (or rolled back) together.
 */
@Component
public class PaymentStatusChangeRecorder {

    private final SavePaymentStatusChangePort savePaymentStatusChangePort;

    public PaymentStatusChangeRecorder(SavePaymentStatusChangePort savePaymentStatusChangePort) {
        this.savePaymentStatusChangePort = savePaymentStatusChangePort;
    }

    public void record(Long paymentId, PaymentStatus from, PaymentStatus to,
                       Long actorUserId, String reason) {
        savePaymentStatusChangePort.append(
                new PaymentStatusChange(null, paymentId, from, to, actorUserId, reason, Instant.now()));
    }
}
