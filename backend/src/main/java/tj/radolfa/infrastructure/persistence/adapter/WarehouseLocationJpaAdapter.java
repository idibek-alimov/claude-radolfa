package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.AssignSkuToBinPort;
import tj.radolfa.application.ports.out.LoadWarehouseLocationPort;
import tj.radolfa.application.ports.out.SaveWarehouseLocationPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.WarehouseBin;
import tj.radolfa.domain.model.WarehouseShelf;
import tj.radolfa.domain.model.WarehouseZone;
import tj.radolfa.infrastructure.persistence.entity.WarehouseBinEntity;
import tj.radolfa.infrastructure.persistence.entity.WarehouseShelfEntity;
import tj.radolfa.infrastructure.persistence.entity.WarehouseZoneEntity;
import tj.radolfa.infrastructure.persistence.mappers.WarehouseLocationMapper;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;
import tj.radolfa.infrastructure.persistence.repository.WarehouseBinRepository;
import tj.radolfa.infrastructure.persistence.repository.WarehouseShelfRepository;
import tj.radolfa.infrastructure.persistence.repository.WarehouseZoneRepository;

import java.util.List;
import java.util.Optional;

@Component
public class WarehouseLocationJpaAdapter
        implements LoadWarehouseLocationPort, SaveWarehouseLocationPort, AssignSkuToBinPort {

    private final WarehouseZoneRepository  zoneRepo;
    private final WarehouseShelfRepository shelfRepo;
    private final WarehouseBinRepository   binRepo;
    private final SkuRepository            skuRepo;
    private final WarehouseLocationMapper  mapper;

    public WarehouseLocationJpaAdapter(WarehouseZoneRepository zoneRepo,
                                       WarehouseShelfRepository shelfRepo,
                                       WarehouseBinRepository binRepo,
                                       SkuRepository skuRepo,
                                       WarehouseLocationMapper mapper) {
        this.zoneRepo  = zoneRepo;
        this.shelfRepo = shelfRepo;
        this.binRepo   = binRepo;
        this.skuRepo   = skuRepo;
        this.mapper    = mapper;
    }

    // ── LoadWarehouseLocationPort ─────────────────────────────────────────────

    @Override
    public List<WarehouseZone> findAllZones() {
        return zoneRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<WarehouseZone> findZoneById(Long id) {
        return zoneRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<WarehouseShelf> findShelvesByZoneId(Long zoneId) {
        return shelfRepo.findByZoneId(zoneId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<WarehouseShelf> findShelfById(Long id) {
        return shelfRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<WarehouseBin> findBinsByShelfId(Long shelfId) {
        return binRepo.findByShelfId(shelfId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<WarehouseBin> findBinById(Long binId) {
        return binRepo.findById(binId).map(mapper::toDomain);
    }

    // ── SaveWarehouseLocationPort ─────────────────────────────────────────────

    @Override
    public WarehouseZone saveZone(WarehouseZone zone) {
        WarehouseZoneEntity entity = mapper.toEntity(zone);
        return mapper.toDomain(zoneRepo.save(entity));
    }

    @Override
    public WarehouseShelf saveShelf(WarehouseShelf shelf) {
        WarehouseShelfEntity entity = mapper.toEntity(shelf);
        entity.setZone(zoneRepo.getReferenceById(shelf.zoneId()));
        return mapper.toDomain(shelfRepo.save(entity));
    }

    @Override
    public WarehouseBin saveBin(WarehouseBin bin) {
        WarehouseBinEntity entity = mapper.toEntity(bin);
        entity.setShelf(shelfRepo.getReferenceById(bin.shelfId()));
        return mapper.toDomain(binRepo.save(entity));
    }

    @Override
    public void deleteZone(Long id) {
        zoneRepo.deleteById(id);
    }

    @Override
    public void deleteShelf(Long id) {
        shelfRepo.deleteById(id);
    }

    @Override
    public void deleteBin(Long id) {
        binRepo.deleteById(id);
    }

    // ── AssignSkuToBinPort ────────────────────────────────────────────────────

    @Override
    public void assign(Long skuId, Long binId) {
        var sku = skuRepo.findById(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("SKU not found: " + skuId));
        WarehouseBinEntity bin = null;
        if (binId != null) {
            bin = binRepo.findById(binId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bin not found: " + binId));
        }
        sku.setBin(bin);
        skuRepo.save(sku);
    }
}
