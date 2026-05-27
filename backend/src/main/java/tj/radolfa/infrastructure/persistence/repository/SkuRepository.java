package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SkuRepository extends JpaRepository<SkuEntity, Long> {

    Optional<SkuEntity> findBySkuCode(String skuCode);

    Optional<SkuEntity> findByBarcode(String barcode);

    List<SkuEntity> findBySkuCodeIn(Collection<String> skuCodes);

    List<SkuEntity> findByListingVariantId(Long listingVariantId);

    List<SkuEntity> findByListingVariantIdIn(List<Long> variantIds);

    @Query("SELECT DISTINCT s.listingVariant.id FROM SkuEntity s WHERE s.skuCode IN :skuCodes")
    List<Long> findVariantIdsByItemCodes(@Param("skuCodes") Collection<String> skuCodes);

    /**
     * Batch-load SKU grid data for a set of variant IDs.
     * Column layout: [0]=variantId, [1]=skuId, [2]=skuCode, [3]=sizeLabel,
     *                [4]=stockQuantity, [5]=originalPrice
     */
    @Query("""
            SELECT s.listingVariant.id, s.id, s.skuCode, s.sizeLabel, s.stockQuantity, s.originalPrice
            FROM SkuEntity s
            WHERE s.listingVariant.id IN :variantIds
            ORDER BY s.listingVariant.id ASC, s.sizeLabel ASC
            """)
    List<Object[]> findGridSkusByVariantIds(@Param("variantIds") List<Long> variantIds);

    /**
     * Full-text warehouse SKU search across skuCode, barcode, and product name.
     * Column layout: [0]=id, [1]=skuCode, [2]=barcode, [3]=sizeLabel,
     *                [4]=stockQuantity, [5]=productName
     * Placements are enriched by SearchSkusService after this query.
     */
    @Query(value = """
            SELECT s.id, s.sku_code, s.barcode, s.size_label, s.stock_quantity, pb.name
            FROM skus s
            JOIN listing_variants lv ON s.listing_variant_id = lv.id
            JOIN product_bases pb    ON lv.product_base_id = pb.id
            WHERE LOWER(s.sku_code) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(s.barcode, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(pb.name) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY pb.name ASC, s.sku_code ASC
            """,
           countQuery = """
            SELECT COUNT(*)
            FROM skus s
            JOIN listing_variants lv ON s.listing_variant_id = lv.id
            JOIN product_bases pb    ON lv.product_base_id = pb.id
            WHERE LOWER(s.sku_code) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(s.barcode, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(pb.name) LIKE LOWER(CONCAT('%', :query, '%'))
            """,
           nativeQuery = true)
    Page<Object[]> searchSkus(@Param("query") String query, Pageable pageable);

    @Modifying
    @Query("UPDATE SkuEntity s SET s.stockQuantity = :qty WHERE s.id = :id")
    void setStockQuantity(@Param("id") Long id, @Param("qty") int qty);
}
