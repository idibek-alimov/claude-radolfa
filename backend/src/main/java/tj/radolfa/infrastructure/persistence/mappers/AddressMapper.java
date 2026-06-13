package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.Address;
import tj.radolfa.infrastructure.persistence.entity.AddressEntity;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    // Explicit mapping: the boolean getter `isDefault()` is read by MapStruct as property
    // "default", which doesn't match the `isDefault` constructor parameter — map it directly.
    @Mapping(target = "isDefault", expression = "java(entity.isDefault())")
    Address toAddress(AddressEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AddressEntity toEntity(Address address);
}
