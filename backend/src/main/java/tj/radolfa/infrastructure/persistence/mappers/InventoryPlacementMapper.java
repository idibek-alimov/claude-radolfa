package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.InventoryPlacement;
import tj.radolfa.infrastructure.persistence.entity.InventoryPlacementEntity;

@Mapper(componentModel = "spring")
public interface InventoryPlacementMapper {

    InventoryPlacement toDomain(InventoryPlacementEntity entity);

    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InventoryPlacementEntity toEntity(InventoryPlacement domain);
}
