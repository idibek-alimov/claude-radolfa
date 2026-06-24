package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.SkuPriceChangeEntity;

public interface SkuPriceChangeJpaRepository extends JpaRepository<SkuPriceChangeEntity, Long> {
}
