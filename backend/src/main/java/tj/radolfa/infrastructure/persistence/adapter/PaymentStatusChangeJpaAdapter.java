package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadPaymentStatusChangePort;
import tj.radolfa.application.ports.out.SavePaymentStatusChangePort;
import tj.radolfa.domain.model.PaymentStatusChange;
import tj.radolfa.infrastructure.persistence.entity.PaymentStatusChangeEntity;
import tj.radolfa.infrastructure.persistence.repository.PaymentStatusChangeJpaRepository;

@Component
public class PaymentStatusChangeJpaAdapter
        implements SavePaymentStatusChangePort, LoadPaymentStatusChangePort {

    private final PaymentStatusChangeJpaRepository repository;

    public PaymentStatusChangeJpaAdapter(PaymentStatusChangeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentStatusChange append(PaymentStatusChange change) {
        var entity = new PaymentStatusChangeEntity(
                null,
                change.paymentId(),
                change.statusFrom(),
                change.statusTo(),
                change.actorUserId(),
                change.reason(),
                change.occurredAt());
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Page<PaymentStatusChange> findByPaymentId(Long paymentId, Pageable pageable) {
        return repository.findByPaymentId(paymentId, pageable).map(this::toDomain);
    }

    private PaymentStatusChange toDomain(PaymentStatusChangeEntity e) {
        return new PaymentStatusChange(
                e.getId(),
                e.getPaymentId(),
                e.getStatusFrom(),
                e.getStatusTo(),
                e.getActorUserId(),
                e.getReason(),
                e.getOccurredAt());
    }
}
