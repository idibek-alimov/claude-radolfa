package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.DeliveryCodeEntity;

import java.time.Instant;
import java.util.Optional;

public interface DeliveryCodeJpaRepository extends JpaRepository<DeliveryCodeEntity, Long> {

    Optional<DeliveryCodeEntity> findFirstByOrderIdAndUsedAtIsNullOrderByCreatedAtDesc(Long orderId);

    Optional<DeliveryCodeEntity> findByCodeAndUsedAtIsNull(String code);

    @Modifying
    @Query("UPDATE DeliveryCodeEntity d SET d.usedAt = :now WHERE d.orderId = :orderId AND d.usedAt IS NULL")
    void markAllAsUsedByOrderId(@Param("orderId") Long orderId, @Param("now") Instant now);
}
