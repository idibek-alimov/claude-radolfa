package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.OrderStatusChangeEntity;

public interface OrderStatusChangeJpaRepository extends JpaRepository<OrderStatusChangeEntity, Long> {
    Page<OrderStatusChangeEntity> findByOrderId(Long orderId, Pageable pageable);
}
