package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.Seller;
import tj.radolfa.infrastructure.persistence.entity.SellerEntity;

@Mapper(componentModel = "spring")
public interface SellerMapper {

    Seller toDomain(SellerEntity entity);

    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SellerEntity toEntity(Seller domain);
}
