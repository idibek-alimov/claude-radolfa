package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.Warehouse;
import tj.radolfa.infrastructure.persistence.entity.WarehouseEntity;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    @Mapping(source = "default", target = "isDefault")
    Warehouse toDomain(WarehouseEntity entity);

    @Mapping(source = "isDefault", target = "default")
    WarehouseEntity toEntity(Warehouse domain);
}
