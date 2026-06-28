package tj.radolfa.application.ports.in.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.PaymentStatusChange;

public interface GetPaymentStatusHistoryUseCase {
    /**
     * Returns the status-change history for the payment associated with the given order.
     *
     * <p>COD orders have no {@code payments} row — this method returns an empty page for them.
     */
    Page<PaymentStatusChange> execute(Long orderId, Pageable pageable);
}
