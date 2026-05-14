package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.DiscountTypeEntity;

import java.util.List;

public interface DiscountTypeRepository extends JpaRepository<DiscountTypeEntity, Long> {

    List<DiscountTypeEntity> findAllByOrderByRankAsc();
}
