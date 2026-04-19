package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.DiscountApplicationEntity;

public interface DiscountApplicationRepository extends JpaRepository<DiscountApplicationEntity, Long> {
}
