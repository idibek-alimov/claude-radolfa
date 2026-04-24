package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.readmodel.CartView;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(
        Long              cartId,
        List<CartItemDto> items,
        BigDecimal        totalAmount,
        int               itemCount,
        String            couponCode
) {
    public static CartDto fromView(CartView view) {
        List<CartItemDto> items = view.items().stream()
                .map(CartItemDto::fromItemView)
                .toList();
        return new CartDto(view.cartId(), items, view.total().amount(), view.itemCount(), view.couponCode());
    }
}
