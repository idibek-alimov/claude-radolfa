package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SkuRepository extends JpaRepository<SkuEntity, Long> {

    Optional<SkuEntity> findBySkuCode(String skuCode);

    List<SkuEntity> findByListingVariantId(Long listingVariantId);

    List<SkuEntity> findByListingVariantIdIn(List<Long> variantIds);

    @Query("SELECT DISTINCT s.listingVariant.id FROM SkuEntity s WHERE s.skuCode IN :skuCodes")
    List<Long> findVariantIdsByItemCodes(@Param("skuCodes") Collection<String> skuCodes);
}
