package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.ProductBaseEntity;

import java.time.Instant;

public interface AdminProductBaseQueryRepository extends JpaRepository<ProductBaseEntity, Long> {

    /**
     * Single query: product base + first variant (lowest id) + that variant's
     * lowest sort_order image. Two LATERAL subqueries replace the previous N+1 pattern.
     * ORDER BY is embedded so pagination stays correct without a Sort on Pageable.
     */
    @Query(value = """
            SELECT
                pb.id               AS productBaseId,
                pb.external_ref     AS externalRef,
                pb.name             AS name,
                pb.status           AS status,
                pb.rejection_reason AS rejectionReason,
                img.image_url       AS primaryImageUrl,
                lv.product_code     AS productCode,
                pb.updated_at       AS updatedAt
            FROM product_bases pb
            LEFT JOIN LATERAL (
                SELECT id, product_code
                FROM listing_variants
                WHERE product_base_id = pb.id
                ORDER BY id ASC LIMIT 1
            ) lv ON true
            LEFT JOIN LATERAL (
                SELECT image_url
                FROM listing_variant_images
                WHERE listing_variant_id = lv.id
                ORDER BY sort_order ASC LIMIT 1
            ) img ON true
            WHERE (:status IS NULL OR pb.status = :status)
              AND (:search = ''
                   OR LOWER(pb.name)         LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(pb.external_ref) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:sellerId IS NULL OR pb.seller_id = :sellerId)
            ORDER BY pb.id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM product_bases pb
            WHERE (:status IS NULL OR pb.status = :status)
              AND (:search = ''
                   OR LOWER(pb.name)         LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(pb.external_ref) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:sellerId IS NULL OR pb.seller_id = :sellerId)
            """,
            nativeQuery = true)
    Page<AdminProductRowProjection> findAdminPage(
            @Param("status") String status,
            @Param("search") String search,
            @Param("sellerId") Long sellerId,
            Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM product_bases WHERE status = :status", nativeQuery = true)
    long countByStatus(@Param("status") String status);

    interface AdminProductRowProjection {
        Long    getProductBaseId();
        String  getExternalRef();
        String  getName();
        String  getStatus();
        String  getRejectionReason();
        String  getPrimaryImageUrl();
        String  getProductCode();
        Instant getUpdatedAt();
    }
}
