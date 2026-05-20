package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.WarehouseBin;
import tj.radolfa.domain.model.WarehouseShelf;
import tj.radolfa.domain.model.WarehouseZone;
import tj.radolfa.infrastructure.persistence.entity.WarehouseBinEntity;
import tj.radolfa.infrastructure.persistence.entity.WarehouseShelfEntity;
import tj.radolfa.infrastructure.persistence.entity.WarehouseZoneEntity;

@Mapper(componentModel = "spring")
public interface WarehouseLocationMapper {

    // ── Zone ──────────────────────────────────────────────────────────────────

    WarehouseZone toDomain(WarehouseZoneEntity entity);

    @Mapping(target = "shelves",   ignore = true)
    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    WarehouseZoneEntity toEntity(WarehouseZone domain);

    // ── Shelf ─────────────────────────────────────────────────────────────────

    @Mapping(source = "zone.id", target = "zoneId")
    WarehouseShelf toDomain(WarehouseShelfEntity entity);

    @Mapping(target = "zone",      ignore = true)
    @Mapping(target = "bins",      ignore = true)
    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    WarehouseShelfEntity toEntity(WarehouseShelf domain);

    // ── Bin ───────────────────────────────────────────────────────────────────

    @Mapping(source = "shelf.id", target = "shelfId")
    WarehouseBin toDomain(WarehouseBinEntity entity);

    @Mapping(target = "shelf",     ignore = true)
    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    WarehouseBinEntity toEntity(WarehouseBin domain);
}
