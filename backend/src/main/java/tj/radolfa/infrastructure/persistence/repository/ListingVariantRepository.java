package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;

import java.util.List;
import java.util.Optional;

public interface ListingVariantRepository extends JpaRepository<ListingVariantEntity, Long> {

        @Query("""
                        SELECT lv FROM ListingVariantEntity lv
                        WHERE lv.productBase.id = :productBaseId AND lv.color.colorKey = :colorKey
                        """)
        Optional<ListingVariantEntity> findByProductBaseIdAndColorKey(
                        @Param("productBaseId") Long productBaseId,
                        @Param("colorKey") String colorKey);

        /**
     * Detail page query: eagerly fetches productBase, category, and color
     * in a single SQL JOIN to avoid N+1 on the detail page.
     */
    @Query("""
                    SELECT lv FROM ListingVariantEntity lv
                    JOIN FETCH lv.productBase pb
                    LEFT JOIN FETCH pb.category
                    JOIN FETCH lv.color
                    WHERE lv.slug = :slug
                    """)
    Optional<ListingVariantEntity> findDetailBySlug(@Param("slug") String slug);

    Optional<ListingVariantEntity> findBySlug(String slug);

        List<ListingVariantEntity> findByProductBaseId(Long productBaseId);

        // ---- Grid queries with SKU aggregates ----
        // Column layout: [0]=id, [1]=slug, [2]=name, [3]=categoryName, [4]=colorKey,
        //                 [5]=webDescription, [6]=topSelling, [7]=priceStart, [8]=priceEnd,
        //                 [9]=totalStock, [10]=colorHexCode

        /**
         * Paginated grid: variant card data with aggregated price/stock from SKUs.
         * Single query, no N+1.
         */
        @Query("""
                        SELECT lv.id, lv.slug, pb.name, pb.category.name, lv.color.colorKey,
                               lv.webDescription, lv.topSelling,
                               COALESCE(MIN(s.salePrice), MIN(s.price)) AS priceStart,
                               COALESCE(MAX(s.salePrice), MAX(s.price)) AS priceEnd,
                               COALESCE(SUM(s.stockQuantity), 0) AS totalStock,
                               lv.color.hexCode
                        FROM ListingVariantEntity lv
                        JOIN lv.productBase pb
                        LEFT JOIN lv.skus s
                        GROUP BY lv.id, lv.slug, pb.name, pb.category.name, lv.color.colorKey,
                                 lv.webDescription, lv.topSelling, lv.color.hexCode
                        ORDER BY lv.id ASC
                        """)
        Page<Object[]> findGridPage(Pageable pageable);

        /**
         * Paginated grid filtered by category IDs (for category browsing with descendants).
         */
        @Query("""
                        SELECT lv.id, lv.slug, pb.name, pb.category.name, lv.color.colorKey,
                               lv.webDescription, lv.topSelling,
                               COALESCE(MIN(s.salePrice), MIN(s.price)) AS priceStart,
                               COALESCE(MAX(s.salePrice), MAX(s.price)) AS priceEnd,
                               COALESCE(SUM(s.stockQuantity), 0) AS totalStock,
                               lv.color.hexCode
                        FROM ListingVariantEntity lv
                        JOIN lv.productBase pb
                        LEFT JOIN lv.skus s
                        WHERE pb.category.id IN :categoryIds
                        GROUP BY lv.id, lv.slug, pb.name, pb.category.name, lv.color.colorKey,
                                 lv.webDescription, lv.topSelling, lv.color.hexCode
                        ORDER BY lv.id ASC
                        """)
        Page<Object[]> findGridByCategoryIds(@Param("categoryIds") List<Long> categoryIds, Pageable pageable);

        /**
         * SQL LIKE fallback search on product name and colour key.
         */
        @Query("""
                        SELECT lv.id, lv.slug, pb.name, pb.category.name, lv.color.colorKey,
                               lv.webDescription, lv.topSelling,
                               COALESCE(MIN(s.salePrice), MIN(s.price)) AS priceStart,
                               COALESCE(MAX(s.salePrice), MAX(s.price)) AS priceEnd,
                               COALESCE(SUM(s.stockQuantity), 0) AS totalStock,
                               lv.color.hexCode
                        FROM ListingVariantEntity lv
                        JOIN lv.productBase pb
                        LEFT JOIN lv.skus s
                        WHERE LOWER(pb.name) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(lv.color.colorKey) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(lv.webDescription) LIKE LOWER(CONCAT('%', :query, '%'))
                        GROUP BY lv.id, lv.slug, pb.name, pb.category.name, lv.color.colorKey,
                                 lv.webDescription, lv.topSelling, lv.color.hexCode
                        ORDER BY lv.id ASC
                        """)
        Page<Object[]> searchGrid(@Param("query") String query, Pageable pageable);

        /**
         * SQL LIKE fallback autocomplete: distinct product names matching the prefix.
         */
        @Query("""
                        SELECT DISTINCT pb.name
                        FROM ListingVariantEntity lv
                        JOIN lv.productBase pb
                        WHERE LOWER(pb.name) LIKE LOWER(CONCAT('%', :prefix, '%'))
                        ORDER BY pb.name ASC
                        """)
        List<String> autocompleteNames(@Param("prefix") String prefix, Pageable pageable);

        // ---- Homepage collection queries ----

        /**
         * Featured listings (manually curated via the {@code featured} flag).
         */
        @Query("""
                        SELECT lv.id, lv.slug, pb.name, pb.category.name, lv.color.colorKey,
                               lv.webDescription, lv.topSelling,
                               COALESCE(MIN(s.salePrice), MIN(s.price)) AS priceStart,
                               COALESCE(MAX(s.salePrice), MAX(s.price)) AS priceEnd,
                               COALESCE(SUM(s.stockQuantity), 0) AS totalStock,
                               lv.color.hexCode
                        FROM ListingVariantEntity lv
                        JOIN lv.productBase pb
                        LEFT JOIN lv.skus s
                        WHERE lv.featured = TRUE
                        GROUP BY lv.id, lv.slug, pb.name, pb.category.name, lv.color.colorKey,
                                 lv.webDescription, lv.topSelling, lv.color.hexCode
                        ORDER BY lv.updatedAt DESC
                        """)
        Page<Object[]> findFeaturedGrid(Pageable pageable);

        /**
         * New arrivals: most recently created variants.
         */
        @Query("""
                        SELECT lv.id, lv.slug, pb.name, pb.category.name, lv.color.colorKey,
                               lv.webDescription, lv.topSelling,
                               COALESCE(MIN(s.salePrice), MIN(s.price)) AS priceStart,
                               COALESCE(MAX(s.salePrice), MAX(s.price)) AS priceEnd,
                               COALESCE(SUM(s.stockQuantity), 0) AS totalStock,
                               lv.color.hexCode
                        FROM ListingVariantEntity lv
                        JOIN lv.productBase pb
                        LEFT JOIN lv.skus s
                        GROUP BY lv.id, lv.slug, pb.name, pb.category.name, lv.color.colorKey,
                                 lv.webDescription, lv.topSelling, lv.color.hexCode, lv.createdAt
                        ORDER BY lv.createdAt DESC
                        """)
        Page<Object[]> findNewArrivalsGrid(Pageable pageable);

        /**
         * On-sale listings: variants that have at least one SKU with an active sale price.
         */
        @Query("""
                        SELECT lv.id, lv.slug, pb.name, pb.category.name, lv.color.colorKey,
                               lv.webDescription, lv.topSelling,
                               COALESCE(MIN(s.salePrice), MIN(s.price)) AS priceStart,
                               COALESCE(MAX(s.salePrice), MAX(s.price)) AS priceEnd,
                               COALESCE(SUM(s.stockQuantity), 0) AS totalStock,
                               lv.color.hexCode
                        FROM ListingVariantEntity lv
                        JOIN lv.productBase pb
                        JOIN lv.skus s
                        WHERE s.salePrice IS NOT NULL AND s.salePrice < s.price
                        GROUP BY lv.id, lv.slug, pb.name, pb.category.name, lv.color.colorKey,
                                 lv.webDescription, lv.topSelling, lv.color.hexCode
                        ORDER BY lv.updatedAt DESC
                        """)
        Page<Object[]> findOnSaleGrid(Pageable pageable);

        /**
         * Sibling variants of the same ProductBase (excluding the current one).
         * Column layout: [0]=id, [1]=slug, [2]=colorKey, [3]=colorHexCode
         */
        @Query("""
                        SELECT lv.id, lv.slug, lv.color.colorKey, lv.color.hexCode
                        FROM ListingVariantEntity lv
                        WHERE lv.productBase.id = :baseId AND lv.id != :excludeId
                        ORDER BY lv.color.colorKey ASC
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
