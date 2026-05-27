package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.ports.out.LoadWarehouseLocationPort;
import tj.radolfa.application.readmodel.InboundQueueItem;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.exception.BinWarehouseMismatchException;
import tj.radolfa.domain.exception.InsufficientPlacementStockException;
import tj.radolfa.domain.model.InventoryPlacement;
import tj.radolfa.domain.model.WarehouseBin;
import tj.radolfa.domain.model.WarehouseShelf;
import tj.radolfa.domain.model.WarehouseZone;
import tj.radolfa.infrastructure.persistence.entity.InventoryPlacementEntity;
import tj.radolfa.infrastructure.persistence.mappers.InventoryPlacementMapper;
import tj.radolfa.infrastructure.persistence.repository.InventoryPlacementRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements all placement mutations. Runs inside the calling service's @Transactional boundary.
 * Every mutating method locks all placement rows for the SKU first (serializes concurrent
 * checkout / putaway / relocation), then recomputes the stock_quantity mirror.
 */
@Component
public class InventoryPlacementAdapter implements InventoryPlacementPort {

    private final InventoryPlacementRepository placementRepo;
    private final SkuRepository                skuRepo;
    private final InventoryPlacementMapper     mapper;
    private final LoadWarehouseLocationPort    locationPort;

    public InventoryPlacementAdapter(InventoryPlacementRepository placementRepo,
                                     SkuRepository skuRepo,
                                     InventoryPlacementMapper mapper,
                                     LoadWarehouseLocationPort locationPort) {
        this.placementRepo = placementRepo;
        this.skuRepo       = skuRepo;
        this.mapper        = mapper;
        this.locationPort  = locationPort;
    }

    // ── InventoryPlacementPort ────────────────────────────────────────────────

    @Override
    public void addToInbound(Long skuId, Long warehouseId, int qty) {
        List<InventoryPlacementEntity> locked = placementRepo.lockForSale(skuId, warehouseId);
        InventoryPlacementEntity inbound = locked.stream()
                .filter(p -> p.getBinId() == null)
                .findFirst()
                .orElse(null);
        if (inbound == null) {
            inbound = new InventoryPlacementEntity(null, skuId, warehouseId, null, qty);
        } else {
            inbound.setQuantity(inbound.getQuantity() + qty);
        }
        placementRepo.save(inbound);
        recomputeMirror(skuId, warehouseId);
    }

    @Override
    public boolean decrementForSale(Long skuId, Long warehouseId, int qty) {
        List<InventoryPlacementEntity> locked = placementRepo.lockForSale(skuId, warehouseId);
        int total = locked.stream().mapToInt(InventoryPlacementEntity::getQuantity).sum();
        if (total < qty) {
            return false;
        }
        int remaining = qty;
        List<InventoryPlacementEntity> modified = new ArrayList<>();
        for (InventoryPlacementEntity row : locked) {
            if (remaining <= 0) break;
            int take = Math.min(row.getQuantity(), remaining);
            if (take > 0) {
                row.setQuantity(row.getQuantity() - take);
                remaining -= take;
                modified.add(row);
            }
        }
        placementRepo.saveAll(modified);
        recomputeMirror(skuId, warehouseId);
        return true;
    }

    @Override
    public void adjustInbound(Long skuId, Long warehouseId, int delta) {
        if (delta == 0) return;
        if (delta > 0) {
            addToInbound(skuId, warehouseId, delta);
            return;
        }
        // delta < 0: drain |delta| inbound-first then bins-desc (same order as lockForSale)
        int toDrain = -delta;
        List<InventoryPlacementEntity> locked = placementRepo.lockForSale(skuId, warehouseId);
        List<InventoryPlacementEntity> modified = new ArrayList<>();
        for (InventoryPlacementEntity row : locked) {
            if (toDrain <= 0) break;
            int take = Math.min(row.getQuantity(), toDrain);
            if (take > 0) {
                row.setQuantity(row.getQuantity() - take);
                toDrain -= take;
                modified.add(row);
            }
        }
        placementRepo.saveAll(modified);
        recomputeMirror(skuId, warehouseId);
    }

    @Override
    public void putaway(Long skuId, Long warehouseId, Long binId, int qty) {
        validateBinInWarehouse(binId, warehouseId);
        List<InventoryPlacementEntity> locked = placementRepo.lockForSale(skuId, warehouseId);

        InventoryPlacementEntity inbound = locked.stream()
                .filter(p -> p.getBinId() == null)
                .findFirst()
                .orElse(null);
        int available = inbound != null ? inbound.getQuantity() : 0;
        if (available < qty) {
            throw new InsufficientPlacementStockException(skuId, null, available, qty);
        }

        inbound.setQuantity(inbound.getQuantity() - qty);
        placementRepo.save(inbound);

        InventoryPlacementEntity binRow = locked.stream()
                .filter(p -> binId.equals(p.getBinId()))
                .findFirst()
                .orElseGet(() -> new InventoryPlacementEntity(null, skuId, warehouseId, binId, 0));
        binRow.setQuantity(binRow.getQuantity() + qty);
        placementRepo.save(binRow);

        recomputeMirror(skuId, warehouseId);
    }

    @Override
    public void relocate(Long skuId, Long warehouseId, Long fromBinId, Long toBinId, int qty) {
        validateBinInWarehouse(fromBinId, warehouseId);
        validateBinInWarehouse(toBinId, warehouseId);
        List<InventoryPlacementEntity> locked = placementRepo.lockForSale(skuId, warehouseId);

        InventoryPlacementEntity fromRow = locked.stream()
                .filter(p -> fromBinId.equals(p.getBinId()))
                .findFirst()
                .orElse(null);
        int available = fromRow != null ? fromRow.getQuantity() : 0;
        if (available < qty) {
            throw new InsufficientPlacementStockException(skuId, fromBinId, available, qty);
        }

        fromRow.setQuantity(fromRow.getQuantity() - qty);
        placementRepo.save(fromRow);

        InventoryPlacementEntity toRow = locked.stream()
                .filter(p -> toBinId.equals(p.getBinId()))
                .findFirst()
                .orElseGet(() -> new InventoryPlacementEntity(null, skuId, warehouseId, toBinId, 0));
        toRow.setQuantity(toRow.getQuantity() + qty);
        placementRepo.save(toRow);

        recomputeMirror(skuId, warehouseId);
    }

    @Override
    public int totalForSku(Long skuId, Long warehouseId) {
        return placementRepo.sumForSku(skuId, warehouseId);
    }

    @Override
    public List<InventoryPlacement> placementsForSku(Long skuId, Long warehouseId) {
        return placementRepo.findBySkuIdAndWarehouseId(skuId, warehouseId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<InboundQueueItem> findInboundQueue(int page, int size, String search) {
        String q = (search == null) ? "" : search.trim();
        Page<Object[]> springPage = placementRepo.findInboundQueue(q, PageRequest.of(page - 1, size));
        List<InboundQueueItem> items = springPage.getContent().stream()
                .map(row -> new InboundQueueItem(
                        ((Number) row[0]).longValue(),   // sku_id
                        (String) row[3],                 // sku_code
                        (String) row[4],                 // barcode
                        (String) row[5],                 // product_name
                        ((Number) row[2]).intValue()))   // quantity
                .toList();
        return new PageResult<>(items, springPage.getTotalElements(), page, size, springPage.isLast());
    }

    @Override
    public boolean hasPlacementsInBin(Long binId) {
        return placementRepo.existsByBinId(binId);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void recomputeMirror(Long skuId, Long warehouseId) {
        skuRepo.setStockQuantity(skuId, placementRepo.sumForSku(skuId, warehouseId));
    }

    private void validateBinInWarehouse(Long binId, Long warehouseId) {
        WarehouseBin bin = locationPort.findBinById(binId)
                .orElseThrow(() -> new BinWarehouseMismatchException(binId, warehouseId));
        WarehouseShelf shelf = locationPort.findShelfById(bin.shelfId())
                .orElseThrow(() -> new BinWarehouseMismatchException(binId, warehouseId));
        WarehouseZone zone = locationPort.findZoneById(shelf.zoneId())
                .orElseThrow(() -> new BinWarehouseMismatchException(binId, warehouseId));
        if (!zone.warehouseId().equals(warehouseId)) {
            throw new BinWarehouseMismatchException(binId, warehouseId);
        }
    }
}
