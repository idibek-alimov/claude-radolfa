package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.DiscountApplicationEntity;

import java.util.Collection;
import java.util.List;

public interface DiscountApplicationRepository extends JpaRepository<DiscountApplicationEntity, Long> {

    @Query("""
        SELECT da.discount.id, COUNT(da)
        FROM DiscountApplicationEntity da
        WHERE da.discount.id IN :discountIds
        GROUP BY da.discount.id
    """)
    List<Object[]> countByDiscountIds(@Param("discountIds") Collection<Long> discountIds);

    @Query("""
        SELECT da.discount.id, COUNT(da)
        FROM DiscountApplicationEntity da
        WHERE da.discount.id IN :discountIds
          AND da.order.user.id = :userId
        GROUP BY da.discount.id
    """)
    List<Object[]> countByDiscountIdsForUser(@Param("discountIds") Collection<Long> discountIds,
                                             @Param("userId") Long userId);
}
