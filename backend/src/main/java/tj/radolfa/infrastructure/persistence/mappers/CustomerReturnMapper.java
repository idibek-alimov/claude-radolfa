package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnItem;
import tj.radolfa.infrastructure.persistence.entity.CustomerReturnEntity;
import tj.radolfa.infrastructure.persistence.entity.CustomerReturnItemEntity;

@Mapper(componentModel = "spring")
public interface CustomerReturnMapper {

    CustomerReturn toDomain(CustomerReturnEntity entity);

    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CustomerReturnEntity toEntity(CustomerReturn domain);

    @Mapping(source = "customerReturn.id", target = "returnId")
    CustomerReturnItem toDomain(CustomerReturnItemEntity entity);

    @Mapping(target = "customerReturn", ignore = true)
    @Mapping(target = "version",        ignore = true)
    @Mapping(target = "createdAt",      ignore = true)
    @Mapping(target = "updatedAt",      ignore = true)
    CustomerReturnItemEntity toEntity(CustomerReturnItem domain);
}
