package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.DiscountEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<DiscountEntity, Long>,
        JpaSpecificationExecutor<DiscountEntity> {

    long countByType_Id(Long typeId);

    /**
     * Returns [DiscountEntity, matchedItemCode] pairs for active discounts covering any of the
     * given item codes. Ordered by type.rank ASC so the highest-priority type wins per item code.
     */
    @Query("""
        SELECT d, ic FROM DiscountEntity d JOIN d.itemCodes ic
        WHERE ic IN :itemCodes
          AND d.disabled = false
          AND d.validFrom <= CURRENT_TIMESTAMP
          AND d.validUpto >= CURRENT_TIMESTAMP
        ORDER BY d.type.rank ASC
    """)
    List<Object[]> findActiveDiscountsByItemCodes(@Param("itemCodes") Collection<String> itemCodes);

    /**
     * Returns [DiscountEntity, matchedItemCode] pairs for active discounts covering the given
     * single item code. Ordered by type.rank ASC.
     */
    @Query("""
        SELECT d, ic FROM DiscountEntity d JOIN d.itemCodes ic
        WHERE ic = :itemCode
          AND d.disabled = false
          AND d.validFrom <= CURRENT_TIMESTAMP
          AND d.validUpto >= CURRENT_TIMESTAMP
        ORDER BY d.type.rank ASC
    """)
    List<Object[]> findActiveDiscountsByItemCode(@Param("itemCode") String itemCode);

    @Query("""
        SELECT DISTINCT ic FROM DiscountEntity d JOIN d.itemCodes ic
        WHERE d.disabled = false
          AND d.validFrom <= CURRENT_TIMESTAMP
          AND d.validUpto >= CURRENT_TIMESTAMP
    """)
    List<String> findActiveItemCodes();

    /**
     * Returns all active discounts that have at least one non-SKU target
     * (i.e., CategoryTarget or SegmentTarget). Bounded set — used for enrichment
     * of the segment/category resolution path.
     */
    @Query("""
        SELECT DISTINCT d FROM DiscountEntity d
        JOIN d.targets t
        WHERE d.disabled = false
          AND d.validFrom <= CURRENT_TIMESTAMP
          AND d.validUpto >= CURRENT_TIMESTAMP
          AND t.targetType <> 'SKU'
    """)
    List<DiscountEntity> findActiveWithAnyNonSkuTarget();

    /**
     * Returns [referenceId, includeDescendants] pairs for all active CATEGORY-targeted discounts.
     * Used to find variant IDs eligible for the "Sale" badge without N+1 lazy loading.
     */
    @Query("""
        SELECT dt.referenceId, dt.includeDescendants
        FROM DiscountEntity d JOIN d.targets dt
        WHERE d.disabled = false
          AND d.validFrom <= CURRENT_TIMESTAMP
          AND d.validUpto >= CURRENT_TIMESTAMP
          AND dt.targetType = 'CATEGORY'
    """)
    List<Object[]> findActiveCategoryTargetRefs();

    @Query("SELECT d FROM DiscountEntity d WHERE LOWER(d.couponCode) = LOWER(:code)")
    Optional<DiscountEntity> findByCouponCodeIgnoreCase(@Param("code") String code);
}
