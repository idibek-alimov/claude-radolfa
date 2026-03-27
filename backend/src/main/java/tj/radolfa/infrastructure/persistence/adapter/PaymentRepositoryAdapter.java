package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadPaymentPort;
import tj.radolfa.application.ports.out.SavePaymentPort;
import tj.radolfa.domain.model.Payment;
import tj.radolfa.infrastructure.persistence.entity.OrderEntity;
import tj.radolfa.infrastructure.persistence.mappers.PaymentMapper;
import tj.radolfa.infrastructure.persistence.repository.PaymentRepository;

import java.util.Optional;

@Component
public class PaymentRepositoryAdapter implements LoadPaymentPort, SavePaymentPort {

    private final PaymentRepository repository;
    private final PaymentMapper mapper;
    private final EntityManager em;

    public PaymentRepositoryAdapter(PaymentRepository repository,
                                    PaymentMapper mapper,
                                    EntityManager em) {
        this.repository = repository;
        this.mapper = mapper;
        this.em = em;
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        return repository.findTopByOrder_IdOrderByCreatedAtDesc(orderId)
                .map(mapper::toPayment);
    }

    @Override
    public Optional<Payment> findByProviderTransactionId(String providerTransactionId) {
        return repository.findByProviderTransactionId(providerTransactionId)
                .map(mapper::toPayment);
    }

    @Override
    public Payment save(Payment payment) {
        var entity = mapper.toEntity(payment);
        entity.setOrder(em.getReference(OrderEntity.class, payment.orderId()));
        var saved = repository.save(entity);
        return mapper.toPayment(saved);
    }
}
