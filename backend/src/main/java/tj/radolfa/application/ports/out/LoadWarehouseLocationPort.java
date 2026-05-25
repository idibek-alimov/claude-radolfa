package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.WarehouseBin;
import tj.radolfa.domain.model.WarehouseShelf;
import tj.radolfa.domain.model.WarehouseZone;

import java.util.List;
import java.util.Optional;

public interface LoadWarehouseLocationPort {

    List<WarehouseZone>      findAllZones();
    Optional<WarehouseZone>  findZoneById(Long id);
    List<WarehouseShelf>     findShelvesByZoneId(Long zoneId);
    Optional<WarehouseShelf> findShelfById(Long id);
    List<WarehouseBin>       findBinsByShelfId(Long shelfId);
    Optional<WarehouseBin>   findBinById(Long binId);
}
