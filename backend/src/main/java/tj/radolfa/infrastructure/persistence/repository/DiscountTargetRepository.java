package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.DiscountTargetEntity;

import java.util.List;

public interface DiscountTargetRepository extends JpaRepository<DiscountTargetEntity, Long> {
    List<DiscountTargetEntity> findAllByDiscountId(Long discountId);
}
