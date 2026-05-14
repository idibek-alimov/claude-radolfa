package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.infrastructure.persistence.entity.DiscountTypeEntity;

@Mapper(componentModel = "spring")
public interface DiscountTypeMapper {

    DiscountTypeEntity toEntity(DiscountType domain);

    DiscountType toDomain(DiscountTypeEntity entity);
}
