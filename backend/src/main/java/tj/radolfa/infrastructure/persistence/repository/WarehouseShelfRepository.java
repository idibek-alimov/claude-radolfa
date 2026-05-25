package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.WarehouseShelfEntity;

import java.util.List;

public interface WarehouseShelfRepository extends JpaRepository<WarehouseShelfEntity, Long> {

    List<WarehouseShelfEntity> findByZoneId(Long zoneId);
}
