package tj.radolfa.infrastructure.persistence.mappers;

import org.springframework.stereotype.Component;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartItem;
import tj.radolfa.domain.model.Money;
import tj.radolfa.infrastructure.persistence.entity.CartEntity;
import tj.radolfa.infrastructure.persistence.entity.CartItemEntity;

import java.util.List;

@Component
public class CartMapper {

    public Cart toCart(CartEntity entity) {
        if (entity == null) return null;
        List<CartItem> items = entity.getItems() != null
                ? entity.getItems().stream().map(this::toCartItem).toList()
                : List.of();
        return new Cart(
                entity.getId(),
                entity.getUserId(),
                entity.getStatus(),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCouponCode()
        );
    }

    public CartItem toCartItem(CartItemEntity entity) {
        if (entity == null) return null;
        return new CartItem(
                entity.getSkuId(),
                entity.getQuantity(),
                Money.of(entity.getUnitPriceSnapshot())
        );
    }
}
