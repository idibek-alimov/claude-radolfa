package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.ManageWarehouseLocationUseCase;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.ports.out.LoadWarehouseLocationPort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.application.ports.out.SaveWarehouseLocationPort;
import tj.radolfa.domain.exception.BinNotEmptyException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.WarehouseBin;
import tj.radolfa.domain.model.WarehouseShelf;
import tj.radolfa.domain.model.WarehouseZone;

import java.util.List;

@Service
@Transactional
public class WarehouseLocationService implements ManageWarehouseLocationUseCase {

    private final LoadWarehouseLocationPort loadPort;
    private final SaveWarehouseLocationPort savePort;
    private final LoadWarehousePort         loadWarehousePort;
    private final InventoryPlacementPort    placementPort;

    public WarehouseLocationService(LoadWarehouseLocationPort loadPort,
                                    SaveWarehouseLocationPort savePort,
                                    LoadWarehousePort loadWarehousePort,
                                    InventoryPlacementPort placementPort) {
        this.loadPort          = loadPort;
        this.savePort          = savePort;
        this.loadWarehousePort = loadWarehousePort;
        this.placementPort     = placementPort;
    }

    // ── Zones ─────────────────────────────────────────────────────────────────

    @Override
    public WarehouseZone createZone(String code, String label) {
        Long warehouseId = loadWarehousePort.findDefault().id();
        return savePort.saveZone(new WarehouseZone(null, warehouseId, code, label));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseZone> listZones() {
        return loadPort.findAllZones();
    }

    @Override
    public void deleteZone(Long zoneId) {
        savePort.deleteZone(zoneId);
    }

    // ── Shelves ───────────────────────────────────────────────────────────────

    @Override
    public WarehouseShelf createShelf(Long zoneId, String code, String label) {
        loadPort.findZoneById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + zoneId));
        return savePort.saveShelf(new WarehouseShelf(null, zoneId, code, label));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseShelf> listShelves(Long zoneId) {
        return loadPort.findShelvesByZoneId(zoneId);
    }

    @Override
    public void deleteShelf(Long shelfId) {
        savePort.deleteShelf(shelfId);
    }

    // ── Bins ──────────────────────────────────────────────────────────────────

    @Override
    public WarehouseBin createBin(Long shelfId, String code) {
        loadPort.findShelfById(shelfId)
                .orElseThrow(() -> new ResourceNotFoundException("Shelf not found: " + shelfId));
        return savePort.saveBin(new WarehouseBin(null, shelfId, code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseBin> listBins(Long shelfId) {
        return loadPort.findBinsByShelfId(shelfId);
    }

    @Override
    public void deleteBin(Long binId) {
        if (placementPort.hasPlacementsInBin(binId)) {
            throw new BinNotEmptyException(binId);
        }
        savePort.deleteBin(binId);
    }
}
