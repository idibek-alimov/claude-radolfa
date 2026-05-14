package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.DiscountApplication;
import tj.radolfa.infrastructure.persistence.entity.DiscountApplicationEntity;

@Mapper(componentModel = "spring")
public interface DiscountApplicationMapper {

    @Mapping(target = "discount", ignore = true)
    @Mapping(target = "order", ignore = true)
    DiscountApplicationEntity toEntity(DiscountApplication domain);

    @Mapping(target = "discountId", source = "discount.id")
    @Mapping(target = "orderId", source = "order.id")
    DiscountApplication toDomain(DiscountApplicationEntity entity);
}
