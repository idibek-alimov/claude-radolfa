package tj.radolfa.infrastructure.persistence.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.InventoryPlacementEntity;

import java.util.List;
import java.util.Optional;

public interface InventoryPlacementRepository extends JpaRepository<InventoryPlacementEntity, Long> {

    /**
     * Locks all placement rows for a SKU in a given warehouse for a sale decrement.
     * Inbound pool (binId IS NULL) comes first; then bins by quantity DESC (drain fullest first).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM InventoryPlacementEntity p " +
           "WHERE p.skuId = :skuId AND p.warehouseId = :wh " +
           "ORDER BY CASE WHEN p.binId IS NULL THEN 0 ELSE 1 END, p.quantity DESC")
    List<InventoryPlacementEntity> lockForSale(@Param("skuId") Long skuId, @Param("wh") Long wh);

    List<InventoryPlacementEntity> findBySkuIdAndWarehouseId(Long skuId, Long warehouseId);

    Optional<InventoryPlacementEntity> findBySkuIdAndWarehouseIdAndBinIdIsNull(Long skuId, Long warehouseId);

    Optional<InventoryPlacementEntity> findBySkuIdAndWarehouseIdAndBinId(Long skuId, Long warehouseId, Long binId);

    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM InventoryPlacementEntity p " +
           "WHERE p.skuId = :skuId AND p.warehouseId = :wh")
    int sumForSku(@Param("skuId") Long skuId, @Param("wh") Long wh);

    /**
     * Inbound queue: SKUs with unassigned stock (bin_id IS NULL, quantity > 0).
     * Searchable by sku_code, barcode, or product name. Server-side paginated.
     */
    @Query(value = """
            SELECT p.sku_id, p.warehouse_id, p.quantity, s.sku_code, s.barcode, pb.name AS product_name
            FROM inventory_placements p
            JOIN skus s              ON s.id  = p.sku_id
            JOIN listing_variants lv ON lv.id = s.listing_variant_id
            JOIN product_bases pb    ON pb.id = lv.product_base_id
            WHERE p.bin_id IS NULL AND p.quantity > 0
              AND (:q = '' OR LOWER(s.sku_code) LIKE LOWER(CONCAT('%', :q, '%'))
                           OR LOWER(s.barcode)  LIKE LOWER(CONCAT('%', :q, '%'))
                           OR LOWER(pb.name)    LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.sku_id
            """,
           countQuery = """
            SELECT COUNT(DISTINCT p.sku_id)
            FROM inventory_placements p
            JOIN skus s              ON s.id  = p.sku_id
            JOIN listing_variants lv ON lv.id = s.listing_variant_id
            JOIN product_bases pb    ON pb.id = lv.product_base_id
            WHERE p.bin_id IS NULL AND p.quantity > 0
              AND (:q = '' OR LOWER(s.sku_code) LIKE LOWER(CONCAT('%', :q, '%'))
                           OR LOWER(s.barcode)  LIKE LOWER(CONCAT('%', :q, '%'))
                           OR LOWER(pb.name)    LIKE LOWER(CONCAT('%', :q, '%')))
            """,
           nativeQuery = true)
    Page<Object[]> findInboundQueue(@Param("q") String q, Pageable pageable);
}
