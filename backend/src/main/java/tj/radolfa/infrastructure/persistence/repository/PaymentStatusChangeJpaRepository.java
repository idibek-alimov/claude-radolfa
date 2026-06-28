package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.PaymentStatusChangeEntity;

public interface PaymentStatusChangeJpaRepository
        extends JpaRepository<PaymentStatusChangeEntity, Long> {

    Page<PaymentStatusChangeEntity> findByPaymentId(Long paymentId, Pageable pageable);
}
