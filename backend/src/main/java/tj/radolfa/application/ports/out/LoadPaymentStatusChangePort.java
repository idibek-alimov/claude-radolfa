package tj.radolfa.application.ports.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.PaymentStatusChange;

public interface LoadPaymentStatusChangePort {
    Page<PaymentStatusChange> findByPaymentId(Long paymentId, Pageable pageable);
}
