package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.PaymentEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findTopByOrder_IdOrderByCreatedAtDesc(Long orderId);

    Optional<PaymentEntity> findByProviderTransactionId(String providerTransactionId);

    @Query(value = """
            SELECT COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN p.amount ELSE 0 END), 0)
                 - COALESCE(SUM(CASE WHEN p.status = 'REFUNDED'  THEN p.amount ELSE 0 END), 0)
            FROM payments p
            JOIN orders o ON p.order_id = o.id
            WHERE o.user_id = :userId
              AND p.completed_at >= :from
              AND p.completed_at < :to
              AND p.status IN ('COMPLETED', 'REFUNDED')
            """, nativeQuery = true)
    BigDecimal calculateNetSpending(@Param("userId") Long userId,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to);
}
