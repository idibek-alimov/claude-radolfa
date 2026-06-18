package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.readmodel.CartView;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(
        Long              cartId,
        List<CartItemDto> items,
        BigDecimal        totalAmount,
        int               itemCount,
        String            couponCode,
        Long              pendingOrderId,
        BigDecimal        subtotal,
        BigDecimal        itemDiscounts,
        BigDecimal        crownTier,
        BigDecimal        shipping,
        BigDecimal        savings
) {
    public static CartDto fromView(CartView view) {
        List<CartItemDto> items = view.items().stream()
                .map(CartItemDto::fromItemView)
                .toList();
        CartView.Summary summary = view.summary();
        return new CartDto(
                view.cartId(),
                items,
                view.total().amount(),
                view.itemCount(),
                view.couponCode(),
                view.pendingOrderId(),
                summary.subtotal().amount(),
                summary.itemDiscounts().amount(),
                summary.crownTier().amount(),
                summary.shipping().amount(),
                summary.savings().amount()
        );
    }
}
