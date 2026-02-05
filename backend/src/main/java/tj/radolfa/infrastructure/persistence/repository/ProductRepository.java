package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tj.radolfa.infrastructure.persistence.entity.ProductEntity;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ProductEntity}.
 */
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    /**
     * Lookup by the ERPNext-assigned identifier (the natural key from ERP's perspective).
     */
    Optional<ProductEntity> findByErpId(String erpId);

    /**
     * Find all products marked as top-selling.
     */
    List<ProductEntity> findByTopSellingTrue();
}
