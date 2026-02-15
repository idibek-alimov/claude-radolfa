package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.OrderEntity;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<OrderEntity> findByErpOrderId(String erpOrderId);
}
