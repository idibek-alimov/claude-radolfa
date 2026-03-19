package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.OrderEntity;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @EntityGraph(attributePaths = {"items", "items.sku"})
    List<OrderEntity> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<OrderEntity> findByExternalOrderId(String externalOrderId);
}
