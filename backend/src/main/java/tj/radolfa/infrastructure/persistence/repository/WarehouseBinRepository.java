package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.WarehouseBinEntity;

import java.util.List;

public interface WarehouseBinRepository extends JpaRepository<WarehouseBinEntity, Long> {

    List<WarehouseBinEntity> findByShelfId(Long shelfId);
}
