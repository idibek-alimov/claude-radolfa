package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.BrandEntity;

public interface BrandRepository extends JpaRepository<BrandEntity, Long> {
}
