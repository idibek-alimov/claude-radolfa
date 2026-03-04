package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartItem;
import tj.radolfa.infrastructure.persistence.entity.CartEntity;
import tj.radolfa.infrastructure.persistence.entity.CartItemEntity;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    /**
     * Maps CartEntity to the Cart domain object.
     * Uses a default method because Cart is a mutable class (not a record)
     * and requires explicit constructor invocation — same pattern as
     * {@code ProductHierarchyMapper#toListingVariant}.
     */
    default Cart toCart(CartEntity entity) {
        if (entity == null) return null;
        List<CartItem> items = entity.getItems() != null
                ? entity.getItems().stream().map(this::toCartItem).toList()
                : new ArrayList<>();
        return new Cart(entity.getUserId(), items);
    }

    /**
     * Maps CartItemEntity to the CartItem domain object.
     * Uses a default method to match the CartItem constructor explicitly.
     */
    default CartItem toCartItem(CartItemEntity entity) {
        if (entity == null) return null;
        return new CartItem(
                entity.getSkuId(),
                entity.getListingSlug(),
                entity.getProductName(),
                entity.getSizeLabel(),
                entity.getImageUrl(),
                entity.getPriceSnapshot(),
                entity.getQuantity());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CartEntity toEntity(Cart cart);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cart", ignore = true)
    CartItemEntity toItemEntity(CartItem item);
}
