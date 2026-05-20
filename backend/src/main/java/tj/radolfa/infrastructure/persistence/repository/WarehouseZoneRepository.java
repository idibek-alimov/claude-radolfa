package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.WarehouseZoneEntity;

public interface WarehouseZoneRepository extends JpaRepository<WarehouseZoneEntity, Long> {
}
