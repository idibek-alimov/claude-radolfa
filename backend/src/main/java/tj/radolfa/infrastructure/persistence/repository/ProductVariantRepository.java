package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.ProductVariantEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {

    Optional<ProductVariantEntity> findByErpVariantCode(String erpVariantCode);

    Optional<ProductVariantEntity> findBySeoSlug(String seoSlug);

    @Query("SELECT v.id FROM ProductVariantEntity v WHERE v.erpVariantCode IN :codes")
    List<Long> findIdsByErpVariantCodeIn(Collection<String> codes);

    // ---- Grid queries ----
    // Each grid query returns one card per (template, color) group.
    // Column layout [0..11]:
    //   [0]=variant_id (representative), [1]=seo_slug, [2]=template name,
    //   [3]=category_name, [4]=min_price, [5]=total_stock,
    //   [6]=description, [7]=top_selling, [8]=featured,
    //   [9]=template_id, [10]=color_key, [11]=images (jsonb text from product_color_images)

    @Query(value = """
            SELECT rep.id, rep.seo_slug, t.name, t.category_name,
                   grp.min_price, grp.total_stock,
                   t.description, t.top_selling, t.featured,
                   t.id AS tid, grp.color_key,
                   COALESCE(pci.images, '[]')
            FROM (
                SELECT v.template_id, v.attributes->>'Color' AS color_key,
                       MIN(v.price) AS min_price, CAST(SUM(v.stock_qty) AS integer) AS total_stock,
                       MIN(v.id) AS representative_id
                FROM product_variants v
                WHERE v.is_active = true
                GROUP BY v.template_id, v.attributes->>'Color'
            ) grp
            JOIN product_templates t ON grp.template_id = t.id
            JOIN product_variants rep ON rep.id = grp.representative_id
            LEFT JOIN product_color_images pci
                 ON pci.template_id = grp.template_id
                AND (pci.color_key = grp.color_key OR (pci.color_key IS NULL AND grp.color_key IS NULL))
            WHERE t.is_active = true
            ORDER BY t.name, grp.color_key
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT DISTINCT v.template_id, v.attributes->>'Color'
                FROM product_variants v
                JOIN product_templates t ON v.template_id = t.id
                WHERE v.is_active = true AND t.is_active = true
            ) sub
            """,
            nativeQuery = true)
    Page<Object[]> findGridPage(Pageable pageable);

    @Query(value = """
            SELECT rep.id, rep.seo_slug, t.name, t.category_name,
                   grp.min_price, grp.total_stock,
                   t.description, t.top_selling, t.featured,
                   t.id AS tid, grp.color_key,
                   COALESCE(pci.images, '[]')
            FROM (
                SELECT v.template_id, v.attributes->>'Color' AS color_key,
                       MIN(v.price) AS min_price, CAST(SUM(v.stock_qty) AS integer) AS total_stock,
                       MIN(v.id) AS representative_id
                FROM product_variants v
                WHERE v.is_active = true
                GROUP BY v.template_id, v.attributes->>'Color'
            ) grp
            JOIN product_templates t ON grp.template_id = t.id
            JOIN product_variants rep ON rep.id = grp.representative_id
            LEFT JOIN product_color_images pci
                 ON pci.template_id = grp.template_id
                AND (pci.color_key = grp.color_key OR (pci.color_key IS NULL AND grp.color_key IS NULL))
            WHERE t.is_active = true AND t.featured = true
            ORDER BY t.name
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT DISTINCT v.template_id, v.attributes->>'Color'
                FROM product_variants v
                JOIN product_templates t ON v.template_id = t.id
                WHERE v.is_active = true AND t.is_active = true AND t.featured = true
            ) sub
            """,
            nativeQuery = true)
    Page<Object[]> findFeaturedGrid(Pageable pageable);

    @Query(value = """
            SELECT rep.id, rep.seo_slug, t.name, t.category_name,
                   grp.min_price, grp.total_stock,
                   t.description, t.top_selling, t.featured,
                   t.id AS tid, grp.color_key,
                   COALESCE(pci.images, '[]')
            FROM (
                SELECT v.template_id, v.attributes->>'Color' AS color_key,
                       MIN(v.price) AS min_price, CAST(SUM(v.stock_qty) AS integer) AS total_stock,
                       MIN(v.id) AS representative_id
                FROM product_variants v
                WHERE v.is_active = true
                GROUP BY v.template_id, v.attributes->>'Color'
            ) grp
            JOIN product_templates t ON grp.template_id = t.id
            JOIN product_variants rep ON rep.id = grp.representative_id
            LEFT JOIN product_color_images pci
                 ON pci.template_id = grp.template_id
                AND (pci.color_key = grp.color_key OR (pci.color_key IS NULL AND grp.color_key IS NULL))
            WHERE t.is_active = true
            ORDER BY rep.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT DISTINCT v.template_id, v.attributes->>'Color'
                FROM product_variants v
                JOIN product_templates t ON v.template_id = t.id
                WHERE v.is_active = true AND t.is_active = true
            ) sub
            """,
            nativeQuery = true)
    Page<Object[]> findNewArrivalsGrid(Pageable pageable);

    @Query(value = """
            SELECT rep.id, rep.seo_slug, t.name, t.category_name,
                   grp.min_price, grp.total_stock,
                   t.description, t.top_selling, t.featured,
                   t.id AS tid, grp.color_key,
                   COALESCE(pci.images, '[]')
            FROM (
                SELECT v.template_id, v.attributes->>'Color' AS color_key,
                       MIN(v.price) AS min_price, CAST(SUM(v.stock_qty) AS integer) AS total_stock,
                       MIN(v.id) AS representative_id
                FROM product_variants v
                WHERE v.is_active = true AND v.id IN :variantIds
                GROUP BY v.template_id, v.attributes->>'Color'
            ) grp
            JOIN product_templates t ON grp.template_id = t.id
            JOIN product_variants rep ON rep.id = grp.representative_id
            LEFT JOIN product_color_images pci
                 ON pci.template_id = grp.template_id
                AND (pci.color_key = grp.color_key OR (pci.color_key IS NULL AND grp.color_key IS NULL))
            WHERE t.is_active = true
            ORDER BY t.name
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT DISTINCT v.template_id, v.attributes->>'Color'
                FROM product_variants v
                JOIN product_templates t ON v.template_id = t.id
                WHERE v.is_active = true AND t.is_active = true AND v.id IN :variantIds
            ) sub
            """,
            nativeQuery = true)
    Page<Object[]> findGridByVariantIds(@Param("variantIds") Collection<Long> variantIds, Pageable pageable);

    @Query(value = """
            SELECT rep.id, rep.seo_slug, t.name, t.category_name,
                   grp.min_price, grp.total_stock,
                   t.description, t.top_selling, t.featured,
                   t.id AS tid, grp.color_key,
                   COALESCE(pci.images, '[]')
            FROM (
                SELECT v.template_id, v.attributes->>'Color' AS color_key,
                       MIN(v.price) AS min_price, CAST(SUM(v.stock_qty) AS integer) AS total_stock,
                       MIN(v.id) AS representative_id
                FROM product_variants v
                WHERE v.is_active = true
                GROUP BY v.template_id, v.attributes->>'Color'
            ) grp
            JOIN product_templates t ON grp.template_id = t.id
            JOIN product_variants rep ON rep.id = grp.representative_id
            LEFT JOIN product_color_images pci
                 ON pci.template_id = grp.template_id
                AND (pci.color_key = grp.color_key OR (pci.color_key IS NULL AND grp.color_key IS NULL))
            WHERE t.is_active = true AND t.category_id IN :categoryIds
            ORDER BY t.name
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT DISTINCT v.template_id, v.attributes->>'Color'
                FROM product_variants v
                JOIN product_templates t ON v.template_id = t.id
                WHERE v.is_active = true AND t.is_active = true AND t.category_id IN :categoryIds
            ) sub
            """,
            nativeQuery = true)
    Page<Object[]> findGridByCategoryIds(@Param("categoryIds") Collection<Long> categoryIds, Pageable pageable);

    @Query(value = """
            SELECT rep.id, rep.seo_slug, t.name, t.category_name,
                   grp.min_price, grp.total_stock,
                   t.description, t.top_selling, t.featured,
                   t.id AS tid, grp.color_key,
                   COALESCE(pci.images, '[]')
            FROM (
                SELECT v.template_id, v.attributes->>'Color' AS color_key,
                       MIN(v.price) AS min_price, CAST(SUM(v.stock_qty) AS integer) AS total_stock,
                       MIN(v.id) AS representative_id
                FROM product_variants v
                WHERE v.is_active = true
                GROUP BY v.template_id, v.attributes->>'Color'
            ) grp
            JOIN product_templates t ON grp.template_id = t.id
            JOIN product_variants rep ON rep.id = grp.representative_id
            LEFT JOIN product_color_images pci
                 ON pci.template_id = grp.template_id
                AND (pci.color_key = grp.color_key OR (pci.color_key IS NULL AND grp.color_key IS NULL))
            WHERE t.is_active = true
              AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(t.category_name) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY t.name
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT DISTINCT v.template_id, v.attributes->>'Color'
                FROM product_variants v
                JOIN product_templates t ON v.template_id = t.id
                WHERE v.is_active = true AND t.is_active = true
                  AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))
                       OR LOWER(t.category_name) LIKE LOWER(CONCAT('%', :query, '%')))
            ) sub
            """,
            nativeQuery = true)
    Page<Object[]> searchGrid(@Param("query") String query, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT t.name
            FROM product_variants v
            JOIN product_templates t ON v.template_id = t.id
            WHERE v.is_active = true AND t.is_active = true
              AND LOWER(t.name) LIKE LOWER(CONCAT(:prefix, '%'))
            ORDER BY t.name
            """, nativeQuery = true)
    List<String> autocompleteNames(@Param("prefix") String prefix, Pageable pageable);

    // ---- Detail queries ----

    /**
     * All variants of same template+color = the "SKUs" (size options).
     */
    @Query(value = """
            SELECT v FROM ProductVariantEntity v
            WHERE v.template.id = :templateId
              AND v.active = true
              AND FUNCTION('jsonb_extract_path_text', v.attributes, 'Color') = :color
            ORDER BY FUNCTION('jsonb_extract_path_text', v.attributes, 'Size')
            """)
    List<ProductVariantEntity> findSizeSiblings(@Param("templateId") Long templateId,
                                                 @Param("color") String color);

    /**
     * Other color groups for the same template (for color swatches).
     * Returns [0]=id, [1]=seo_slug, [2]=color_key, [3]=images (jsonb text from product_color_images).
     */
    @Query(value = """
            SELECT DISTINCT ON (v.attributes->>'Color')
                   v.id, v.seo_slug, v.attributes->>'Color' AS color_key,
                   COALESCE(pci.images, '[]')
            FROM product_variants v
            LEFT JOIN product_color_images pci
                 ON pci.template_id = v.template_id
                AND (pci.color_key = v.attributes->>'Color'
                     OR (pci.color_key IS NULL AND v.attributes->>'Color' IS NULL))
            WHERE v.template_id = :templateId
              AND v.is_active = true
              AND v.attributes->>'Color' IS DISTINCT FROM :excludeColor
            ORDER BY v.attributes->>'Color', v.id
            """, nativeQuery = true)
    List<Object[]> findColorSiblings(@Param("templateId") Long templateId,
                                      @Param("excludeColor") String excludeColor);
}
