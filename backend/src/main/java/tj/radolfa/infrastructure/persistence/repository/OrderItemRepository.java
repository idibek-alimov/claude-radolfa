package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.OrderItemEntity;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    @Query("""
        SELECT lv.id
        FROM OrderItemEntity oi
        JOIN oi.sku s
        JOIN s.listingVariant lv
        JOIN lv.productBase pb
        WHERE pb.status = tj.radolfa.domain.model.ProductStatus.ACTIVE
        GROUP BY lv.id
        ORDER BY SUM(oi.quantity) DESC
        """)
    List<Long> findTopSellingVariantIds(Pageable pageable);

    @Query("""
        SELECT COUNT(DISTINCT lv.id)
        FROM OrderItemEntity oi
        JOIN oi.sku s
        JOIN s.listingVariant lv
        JOIN lv.productBase pb
        WHERE pb.status = tj.radolfa.domain.model.ProductStatus.ACTIVE
        """)
    long countTopSellingVariants();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE OrderItemEntity oi
           SET oi.quantityPicked = oi.quantityPicked + 1,
               oi.pickedAt = CASE WHEN oi.quantityPicked + 1 = oi.quantity
                                  THEN CURRENT_TIMESTAMP ELSE oi.pickedAt END,
               oi.pickedByUserId = CASE WHEN oi.quantityPicked + 1 = oi.quantity
                                  THEN :actorUserId ELSE oi.pickedByUserId END
         WHERE oi.id = :id AND oi.quantityPicked < oi.quantity
        """)
    int incrementPicked(@Param("id") Long id, @Param("actorUserId") Long actorUserId);
}
