package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.PaymentSagaLogEntity;

public interface PaymentSagaLogRepository extends JpaRepository<PaymentSagaLogEntity, Long> {
}
