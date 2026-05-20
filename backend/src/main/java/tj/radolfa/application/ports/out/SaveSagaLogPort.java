package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PaymentSagaLog;

public interface SaveSagaLogPort {
    void save(PaymentSagaLog entry);
}
