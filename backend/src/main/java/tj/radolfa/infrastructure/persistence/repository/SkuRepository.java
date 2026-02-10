package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;

import java.util.List;
import java.util.Optional;

public interface SkuRepository extends JpaRepository<SkuEntity, Long> {

    Optional<SkuEntity> findByErpItemCode(String erpItemCode);

    List<SkuEntity> findByListingVariantId(Long listingVariantId);
}
