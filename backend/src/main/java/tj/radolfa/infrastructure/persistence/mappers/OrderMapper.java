package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.infrastructure.persistence.entity.OrderEntity;
import tj.radolfa.infrastructure.persistence.entity.OrderItemEntity;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "user.id", target = "userId")
    Order toOrder(OrderEntity entity);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    OrderEntity toEntity(Order order);

    /** Updates an existing managed entity in-place, preserving version/audit/items fields. */
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "items", ignore = true)
    void updateEntity(Order order, @MappingTarget OrderEntity entity);

    @Mapping(target = "price", source = "priceAtPurchase")
    @Mapping(target = "skuId", source = "sku.id")
    @Mapping(target = "listingVariantId", source = "sku.listingVariant.id")
    OrderItem toOrderItem(OrderItemEntity entity);

    @Mapping(target = "priceAtPurchase", source = "price")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "sku", ignore = true)
    OrderItemEntity toEntity(OrderItem item);

    // ---- Money <-> BigDecimal bridge --------------------------------

    default Money bigDecimalToMoney(BigDecimal value) {
        return Money.of(value);
    }

    default BigDecimal moneyToBigDecimal(Money money) {
        return money != null ? money.amount() : null;
    }
}
