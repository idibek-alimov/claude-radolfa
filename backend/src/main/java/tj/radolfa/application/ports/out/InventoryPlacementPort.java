package tj.radolfa.application.ports.out;

import tj.radolfa.application.readmodel.InboundQueueItem;
import tj.radolfa.domain.model.InventoryPlacement;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.PlacementView;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface InventoryPlacementPort {

    /** Adds units to the (sku, warehouse) inbound pool. Creates the row if absent. */
    void addToInbound(Long skuId, Long warehouseId, int qty);

    /**
     * Drains qty for a sale: inbound pool first, then bins by quantity DESC.
     * Locks the SKU's placement rows (FOR UPDATE). Returns false if total
     * available &lt; qty (caller throws InsufficientStockException).
     */
    boolean decrementForSale(Long skuId, Long warehouseId, int qty);

    /** Moves qty from inbound pool to a bin. Validates availability + bin/warehouse. */
    void putaway(Long skuId, Long warehouseId, Long binId, int qty);

    /** Moves qty bin → bin. Validates source availability + both bins' warehouse. */
    void relocate(Long skuId, Long warehouseId, Long fromBinId, Long toBinId, int qty);

    /**
     * Net adjust the inbound pool by delta (for MANUAL_ADJUSTMENT). delta may be negative.
     * Positive adds to inbound; negative drains inbound-first then bins-desc.
     */
    void adjustInbound(Long skuId, Long warehouseId, int delta);

    int totalForSku(Long skuId, Long warehouseId);

    List<InventoryPlacement> placementsForSku(Long skuId, Long warehouseId);

    /** Paginated inbound queue: SKUs with bin_id IS NULL and quantity > 0. */
    PageResult<InboundQueueItem> findInboundQueue(int page, int size, String search);

    /** Returns true if any placement row references this bin (qty may be 0). */
    boolean hasPlacementsInBin(Long binId);

    /** Resolved display placements for a single SKU. Bins first (qty DESC), inbound last. qty > 0 only. */
    List<PlacementView> placementViewsForSku(Long skuId, Long warehouseId);

    /**
     * Batch resolved display placements keyed by SKU id. Guard empty skuIds before calling.
     * Bins first (qty DESC), inbound last, per SKU. qty > 0 only.
     */
    Map<Long, List<PlacementView>> placementViewsForSkus(Collection<Long> skuIds, Long warehouseId);
}
