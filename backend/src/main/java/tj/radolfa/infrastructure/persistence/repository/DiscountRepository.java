package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.DiscountEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<DiscountEntity, Long> {

    Optional<DiscountEntity> findByErpPricingRuleId(String erpPricingRuleId);

    @org.springframework.transaction.annotation.Transactional
    void deleteByErpPricingRuleId(String erpPricingRuleId);

    @Query("""
        SELECT d FROM DiscountEntity d
        WHERE d.itemCode = :itemCode
          AND d.disabled = false
          AND d.validFrom <= CURRENT_TIMESTAMP
          AND d.validUpto >= CURRENT_TIMESTAMP
        ORDER BY d.discountValue DESC
    """)
    List<DiscountEntity> findActiveByItemCode(@Param("itemCode") String itemCode);

    @Query("""
        SELECT d FROM DiscountEntity d
        WHERE d.itemCode IN :itemCodes
          AND d.disabled = false
          AND d.validFrom <= CURRENT_TIMESTAMP
          AND d.validUpto >= CURRENT_TIMESTAMP
        ORDER BY d.discountValue DESC
    """)
    List<DiscountEntity> findActiveByItemCodes(@Param("itemCodes") Collection<String> itemCodes);

    @Query("""
        SELECT DISTINCT d.itemCode FROM DiscountEntity d
        WHERE d.disabled = false
          AND d.validFrom <= CURRENT_TIMESTAMP
          AND d.validUpto >= CURRENT_TIMESTAMP
    """)
    List<String> findActiveItemCodes();
}
