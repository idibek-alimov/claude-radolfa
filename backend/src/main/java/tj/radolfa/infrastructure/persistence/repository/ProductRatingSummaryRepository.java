package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.ProductRatingSummaryEntity;

public interface ProductRatingSummaryRepository extends JpaRepository<ProductRatingSummaryEntity, Long> {
}
