package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PaymentStatusChange;

public interface SavePaymentStatusChangePort {
    PaymentStatusChange append(PaymentStatusChange change);
}
