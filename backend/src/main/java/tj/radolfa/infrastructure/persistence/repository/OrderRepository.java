package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.infrastructure.persistence.entity.OrderEntity;

import java.util.Collection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @EntityGraph(attributePaths = {"items", "items.sku"})
    List<OrderEntity> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<OrderEntity> findByExternalOrderId(String externalOrderId);

    @EntityGraph(attributePaths = {"items", "items.sku"})
    List<OrderEntity> findByUser_IdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status, Pageable pageable);

    // ── Admin summary queries ──────────────────────────────────────────────────

    @Query("SELECT COUNT(o) FROM OrderEntity o")
    long countAllOrders();

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.createdAt >= :since")
    long countOrdersSince(@Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderEntity o WHERE o.createdAt >= :from")
    BigDecimal sumRevenueSince(@Param("from") Instant from);

    @Query("SELECT o FROM OrderEntity o JOIN FETCH o.user ORDER BY o.createdAt DESC")
    List<OrderEntity> findMostRecent(Pageable pageable);

    // ── New-customer detection ────────────────────────────────────────────────

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.user.id = :userId AND o.status NOT IN :excluded")
    long countConfirmedOrdersByUserId(@Param("userId") Long userId,
                                     @Param("excluded") Collection<OrderStatus> excluded);

    // ── Purchase verification ──────────────────────────────────────────────────

    @Query(value = """
            SELECT COUNT(*) > 0
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            JOIN skus s ON s.id = oi.sku_id
            WHERE o.user_id = :userId
              AND o.status = 'DELIVERED'
              AND s.listing_variant_id = :listingVariantId
            """, nativeQuery = true)
    boolean hasPurchasedVariant(@Param("userId") Long userId,
                                @Param("listingVariantId") Long listingVariantId);
}
