package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.PaymentEntity;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findTopByOrder_IdOrderByCreatedAtDesc(Long orderId);

    Optional<PaymentEntity> findByProviderTransactionId(String providerTransactionId);
}
