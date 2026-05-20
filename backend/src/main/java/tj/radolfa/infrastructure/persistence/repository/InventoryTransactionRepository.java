package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.InventoryTransactionEntity;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionEntity, Long> {
    Page<InventoryTransactionEntity> findBySkuIdOrderByOccurredAtDesc(Long skuId, Pageable pageable);
}
