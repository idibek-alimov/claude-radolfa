package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.DiscountEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<DiscountEntity, Long> {

    Optional<DiscountEntity> findByExternalRuleId(String externalRuleId);

    @org.springframework.transaction.annotation.Transactional
    void deleteByExternalRuleId(String externalRuleId);

    /**
     * Returns [DiscountEntity, matchedItemCode] pairs for active discounts covering any of the
     * given item codes. Ordered by discountValue DESC so first occurrence per item code is best.
     * Uses a JOIN on the discount_items join table — no secondary lazy-load needed.
     */
    @Query("""
        SELECT d, ic FROM DiscountEntity d JOIN d.itemCodes ic
        WHERE ic IN :itemCodes
          AND d.disabled = false
          AND d.validFrom <= CURRENT_TIMESTAMP
          AND d.validUpto >= CURRENT_TIMESTAMP
        ORDER BY d.discountValue DESC
    """)
    List<Object[]> findActiveDiscountsByItemCodes(@Param("itemCodes") Collection<String> itemCodes);

    /**
     * Returns [DiscountEntity, matchedItemCode] pairs for active discounts covering the given
     * single item code.
     */
    @Query("""
        SELECT d, ic FROM DiscountEntity d JOIN d.itemCodes ic
        WHERE ic = :itemCode
          AND d.disabled = false
          AND d.validFrom <= CURRENT_TIMESTAMP
          AND d.validUpto >= CURRENT_TIMESTAMP
        ORDER BY d.discountValue DESC
    """)
    List<Object[]> findActiveDiscountsByItemCode(@Param("itemCode") String itemCode);

    @Query("""
        SELECT DISTINCT ic FROM DiscountEntity d JOIN d.itemCodes ic
        WHERE d.disabled = false
          AND d.validFrom <= CURRENT_TIMESTAMP
          AND d.validUpto >= CURRENT_TIMESTAMP
    """)
    List<String> findActiveItemCodes();
}
