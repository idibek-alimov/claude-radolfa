package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.infrastructure.persistence.entity.OrderEntity;
import tj.radolfa.infrastructure.persistence.entity.OrderItemEntity;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    Order toOrder(OrderEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderEntity toEntity(Order order);

    OrderItem toOrderItem(OrderItemEntity entity);

    @Mapping(target = "order", ignore = true)
    OrderItemEntity toEntity(OrderItem item);
}
