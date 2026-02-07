package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.infrastructure.persistence.entity.OrderEntity;
import tj.radolfa.infrastructure.persistence.entity.OrderItemEntity;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    /** Entity -> Domain. Extracts userId from the ManyToOne relationship. */
    @Mapping(source = "user.id", target = "userId")
    Order toOrder(OrderEntity entity);

    /**
     * Domain -> Entity.
     * The {@code user} relationship is set by the adapter (requires a managed reference).
     * Audit fields and version are managed by JPA lifecycle hooks.
     */
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    OrderEntity toEntity(Order order);

    OrderItem toOrderItem(OrderItemEntity entity);

    @Mapping(target = "order", ignore = true)
    OrderItemEntity toEntity(OrderItem item);
}
