package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ListingVariantRepository extends JpaRepository<ListingVariantEntity, Long> {

        Optional<ListingVariantEntity> findByProductBaseIdAndColorKey(Long productBaseId, String colorKey);

        Optional<ListingVariantEntity> findBySlug(String slug);

        List<ListingVariantEntity> findByProductBaseId(Long productBaseId);

        // ---- Grid queries with SKU aggregates ----

        /**
         * Paginated grid: variant card data with aggregated price/stock from SKUs.
         * Single query, no N+1.
         */
        @Query("""
                        SELECT lv.id, lv.slug, pb.name, lv.colorKey, lv.webDescription, lv.topSelling,
                               COALESCE(MIN(s.salePrice), MIN(s.price)) AS priceStart,
                               COALESCE(MAX(s.salePrice), MAX(s.price)) AS priceEnd,
                               COALESCE(SUM(s.stockQuantity), 0) AS totalStock
                        FROM ListingVariantEntity lv
                        JOIN lv.productBase pb
                        LEFT JOIN lv.skus s
                        GROUP BY lv.id, lv.slug, pb.name, lv.colorKey, lv.webDescription, lv.topSelling
                        ORDER BY lv.id ASC
                        """)
        Page<Object[]> findGridPage(Pageable pageable);

        /**
         * SQL LIKE fallback search on product name and colour key.
         */
        @Query("""
                        SELECT lv.id, lv.slug, pb.name, lv.colorKey, lv.webDescription, lv.topSelling,
                               COALESCE(MIN(s.salePrice), MIN(s.price)) AS priceStart,
                               COALESCE(MAX(s.salePrice), MAX(s.price)) AS priceEnd,
                               COALESCE(SUM(s.stockQuantity), 0) AS totalStock
                        FROM ListingVariantEntity lv
                        JOIN lv.productBase pb
                        LEFT JOIN lv.skus s
                        WHERE LOWER(pb.name) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(lv.colorKey) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(lv.webDescription) LIKE LOWER(CONCAT('%', :query, '%'))
                        GROUP BY lv.id, lv.slug, pb.name, lv.colorKey, lv.webDescription, lv.topSelling
                        ORDER BY lv.id ASC
                        """)
        Page<Object[]> searchGrid(@Param("query") String query, Pageable pageable);

        /**
         * Sibling variants of the same ProductBase (excluding the current one).
         */
        @Query("""
                        SELECT lv.id, lv.slug, lv.colorKey
                        FROM ListingVariantEntity lv
                        WHERE lv.productBase.id = :baseId AND lv.id != :excludeId
                        ORDER BY lv.colorKey ASC
                        """)
        List<Object[]> findSiblings(@Param("baseId") Long baseId, @Param("excludeId") Long excludeId);

        /**
         * Batch-load image URLs for a set of variant IDs, ordered by sort_order.
         */
        @Query("""
                        SELECT img.listingVariant.id, img.imageUrl
                        FROM ListingVariantImageEntity img
                        WHERE img.listingVariant.id IN :variantIds
                        ORDER BY img.sortOrder ASC
                        """)
        List<Object[]> findImagesByVariantIds(@Param("variantIds") List<Long> variantIds);
}
