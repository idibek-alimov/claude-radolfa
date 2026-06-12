package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.OrderItemEntity;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Native paged query for a seller's own order items (Phase 5 — Seller Orders).
 * The {@code ORDER BY} clause is injected via {@link Pageable}; the service layer
 * whitelists the sort column before building the Pageable to prevent SQL injection.
 */
public interface SellerOrderItemQueryRepository extends JpaRepository<OrderItemEntity, Long> {

    @Query(value = """
            SELECT
                oi.id                  AS orderItemId,
                o.id                   AS orderId,
                o.status               AS orderStatus,
                o.created_at           AS orderCreatedAt,
                oi.product_name        AS productName,
                oi.sku_code            AS skuCode,
                oi.quantity            AS quantity,
                oi.price_at_purchase   AS price
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.seller_id = :sellerId
              AND (:search = ''
                   OR LOWER(oi.product_name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(oi.sku_code)     LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.seller_id = :sellerId
              AND (:search = ''
                   OR LOWER(oi.product_name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(oi.sku_code)     LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            nativeQuery = true)
    Page<SellerOrderItemProjection> findBySellerId(
            @Param("sellerId") Long sellerId,
            @Param("search") String search,
            Pageable pageable);

    interface SellerOrderItemProjection {
        Long      getOrderItemId();
        Long      getOrderId();
        String    getOrderStatus();
        Instant   getOrderCreatedAt();
        String    getProductName();
        String    getSkuCode();
        int       getQuantity();
        BigDecimal getPrice();
    }
}
