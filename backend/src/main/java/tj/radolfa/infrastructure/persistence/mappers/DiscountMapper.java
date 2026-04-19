package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.infrastructure.persistence.entity.DiscountEntity;

@Mapper(componentModel = "spring", uses = {DiscountTypeMapper.class})
public interface DiscountMapper {

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "itemCodes", ignore = true)
    @Mapping(target = "targets", ignore = true)
    @Mapping(target = "amountType", expression = "java(domain.amountType().name())")
    DiscountEntity toEntity(Discount domain);
}
