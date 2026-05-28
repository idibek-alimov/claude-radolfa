package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.ProductBaseEntity;

import java.util.Optional;

public interface ProductBaseRepository extends JpaRepository<ProductBaseEntity, Long> {

    Optional<ProductBaseEntity> findByExternalRef(String externalRef);

    @Modifying
    @Query("UPDATE ProductBaseEntity p SET p.status = tj.radolfa.domain.model.ProductStatus.ACTIVE " +
           "WHERE p.id = :id AND p.status = tj.radolfa.domain.model.ProductStatus.AWAITING_STOCK")
    int activateIfAwaitingStock(@Param("id") Long id);
}
