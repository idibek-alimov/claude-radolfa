package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.OrderEntity;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
