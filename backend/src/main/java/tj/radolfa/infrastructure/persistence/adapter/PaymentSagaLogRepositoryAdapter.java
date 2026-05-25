package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.out.SaveSagaLogPort;
import tj.radolfa.domain.model.PaymentSagaLog;
import tj.radolfa.infrastructure.persistence.entity.PaymentSagaLogEntity;
import tj.radolfa.infrastructure.persistence.repository.PaymentSagaLogRepository;

@Component
public class PaymentSagaLogRepositoryAdapter implements SaveSagaLogPort {

    private final PaymentSagaLogRepository repository;

    public PaymentSagaLogRepositoryAdapter(PaymentSagaLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(PaymentSagaLog entry) {
        repository.save(new PaymentSagaLogEntity(
                null,
                entry.providerTransactionId(),
                entry.stepName(),
                entry.outcome(),
                entry.errorMessage(),
                entry.executedAt()));
    }
}
