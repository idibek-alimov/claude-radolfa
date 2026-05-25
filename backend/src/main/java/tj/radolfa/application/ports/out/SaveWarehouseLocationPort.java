package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.WarehouseBin;
import tj.radolfa.domain.model.WarehouseShelf;
import tj.radolfa.domain.model.WarehouseZone;

public interface SaveWarehouseLocationPort {

    WarehouseZone  saveZone(WarehouseZone zone);
    WarehouseShelf saveShelf(WarehouseShelf shelf);
    WarehouseBin   saveBin(WarehouseBin bin);
    void           deleteZone(Long id);
    void           deleteShelf(Long id);
    void           deleteBin(Long id);
}
