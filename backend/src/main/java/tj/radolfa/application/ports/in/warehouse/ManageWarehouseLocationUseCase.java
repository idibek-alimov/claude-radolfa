package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.domain.model.WarehouseBin;
import tj.radolfa.domain.model.WarehouseShelf;
import tj.radolfa.domain.model.WarehouseZone;

import java.util.List;

public interface ManageWarehouseLocationUseCase {

    // ── Zones ─────────────────────────────────────────────────────────────────

    WarehouseZone       createZone(String code, String label);
    List<WarehouseZone> listZones();
    void                deleteZone(Long zoneId);

    // ── Shelves ───────────────────────────────────────────────────────────────

    WarehouseShelf       createShelf(Long zoneId, String code, String label);
    List<WarehouseShelf> listShelves(Long zoneId);
    void                 deleteShelf(Long shelfId);

    // ── Bins ──────────────────────────────────────────────────────────────────

    WarehouseBin       createBin(Long shelfId, String code);
    List<WarehouseBin> listBins(Long shelfId);
    void               deleteBin(Long binId);
}
