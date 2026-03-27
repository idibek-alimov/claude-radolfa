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

    /**
     * Batch-load SKU grid data for a set of variant IDs.
     * Column layout: [0]=variantId, [1]=skuId, [2]=skuCode, [3]=sizeLabel,
     *                [4]=stockQuantity, [5]=originalPrice
     */
    @Query("""
            SELECT s.listingVariant.id, s.id, s.skuCode, s.sizeLabel, s.stockQuantity, s.originalPrice
            FROM SkuEntity s
            WHERE s.listingVariant.id IN :variantIds
            ORDER BY s.listingVariant.id ASC, s.sizeLabel ASC
            """)
    List<Object[]> findGridSkusByVariantIds(@Param("variantIds") List<Long> variantIds);
}
