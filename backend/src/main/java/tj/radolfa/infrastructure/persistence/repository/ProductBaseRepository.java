package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.ProductBaseEntity;

import java.util.List;
import java.util.Optional;

public interface ProductBaseRepository extends JpaRepository<ProductBaseEntity, Long> {

    Optional<ProductBaseEntity> findByExternalRef(String externalRef);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductBaseEntity p SET p.status = tj.radolfa.domain.model.ProductStatus.ACTIVE " +
           "WHERE p.id = :id AND p.status = tj.radolfa.domain.model.ProductStatus.AWAITING_STOCK")
    int activateIfAwaitingStock(@Param("id") Long id);

    /** Returns [productBaseId, sellerId] pairs for the given IDs, skipping Radolfa-owned (sellerId IS NULL). */
    @Query("SELECT pb.id, pb.sellerId FROM ProductBaseEntity pb WHERE pb.id IN :ids AND pb.sellerId IS NOT NULL")
    List<Object[]> findSellerIdsByIds(@Param("ids") List<Long> ids);

    /** Returns [productBaseId, brandId, brandName] triples for the given IDs, skipping product bases without a brand. */
    @Query("SELECT pb.id, pb.brand.id, pb.brand.name FROM ProductBaseEntity pb WHERE pb.id IN :ids AND pb.brand IS NOT NULL")
    List<Object[]> findBrandsByIds(@Param("ids") List<Long> ids);
}
