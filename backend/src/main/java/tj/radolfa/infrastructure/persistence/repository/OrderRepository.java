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

public interface OrderRepository extends JpaRepository<OrderEntity, Long>,
                                          org.springframework.data.jpa.repository.JpaSpecificationExecutor<OrderEntity> {
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

    @EntityGraph(attributePaths = {"items", "items.sku"})
    List<OrderEntity> findByCourierIdAndStatusInOrderByCreatedAtAsc(Long courierId,
                                                                    Collection<OrderStatus> statuses);

    @EntityGraph(attributePaths = {"items", "items.sku"})
    List<OrderEntity> findByPickpointIdAndStatusOrderByCreatedAtAsc(Long pickpointId, OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.sku"})
    List<OrderEntity> findByPickpointIdAndStatusInOrderByCreatedAtAsc(Long pickpointId,
                                                                       Collection<OrderStatus> statuses);

    @EntityGraph(attributePaths = {"items", "items.sku"})
    org.springframework.data.domain.Page<OrderEntity> findByPickpointIdAndStatusIn(
            Long pickpointId,
            Collection<OrderStatus> statuses,
            org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.sku"})
    org.springframework.data.domain.Page<OrderEntity> findByCourierIdAndStatusIn(
            Long courierId,
            Collection<OrderStatus> statuses,
            org.springframework.data.domain.Pageable pageable);

    // ── Pickpoint expiry queries ──────────────────────────────────────────────

    @EntityGraph(attributePaths = {"items", "items.sku"})
    List<OrderEntity> findByStatusAndReadyForPickupAtLessThan(OrderStatus status, Instant cutoff);

    @EntityGraph(attributePaths = {"items", "items.sku"})
    List<OrderEntity> findByStatusAndReadyForPickupAtBetween(OrderStatus status, Instant start, Instant end);

    // ── Pickpoint summary aggregation ────────────────────────────────────────

    @Query(value = """
            SELECT pickpoint_id,
                   SUM(CASE WHEN status='SHIPPED'         THEN 1 ELSE 0 END),
                   SUM(CASE WHEN status='READY_FOR_PICKUP' THEN 1 ELSE 0 END),
                   SUM(CASE WHEN status='READY_FOR_PICKUP' AND ready_for_pickup_at < :cutoff THEN 1 ELSE 0 END),
                   SUM(CASE WHEN status='RETURN_INITIATED'  THEN 1 ELSE 0 END)
            FROM orders
            WHERE pickpoint_id IS NOT NULL
            GROUP BY pickpoint_id
            """, nativeQuery = true)
    List<Object[]> countByPickpointAndStatus(@Param("cutoff") Instant cutoff);

    // ── Fleet summary aggregation ─────────────────────────────────────────────

    @Query(value = """
            SELECT courier_id,
                   SUM(CASE WHEN status = 'DELIVERED' AND delivered_at >= :since THEN 1 ELSE 0 END),
                   SUM(CASE WHEN status IN ('SHIPPED', 'OUT_FOR_DELIVERY')           THEN 1 ELSE 0 END),
                   SUM(CASE WHEN status = 'DELIVERY_ATTEMPTED'                       THEN 1 ELSE 0 END)
            FROM orders
            WHERE courier_id IS NOT NULL
            GROUP BY courier_id
            """, nativeQuery = true)
    List<Object[]> aggregateFleetStats(@Param("since") Instant since);

    // ── Abandoned payment sweep ───────────────────────────────────────────────

    @EntityGraph(attributePaths = {"items"})
    List<OrderEntity> findByStatusAndCreatedAtLessThan(OrderStatus status, Instant cutoff);
}
