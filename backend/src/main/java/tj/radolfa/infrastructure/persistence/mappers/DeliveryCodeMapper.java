package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import tj.radolfa.domain.model.DeliveryCode;
import tj.radolfa.infrastructure.persistence.entity.DeliveryCodeEntity;

@Mapper(componentModel = "spring")
public interface DeliveryCodeMapper {

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DeliveryCodeEntity toEntity(DeliveryCode domain);

    DeliveryCode toDomain(DeliveryCodeEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(DeliveryCode src, @MappingTarget DeliveryCodeEntity target);
}
