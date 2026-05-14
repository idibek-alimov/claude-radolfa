package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.readmodel.CartView;

import java.math.BigDecimal;

public record CartItemDto(
        Long       skuId,
        String     productName,
        String     colorName,
        String     sizeLabel,
        String     imageUrl,
        int        quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        int        availableStock,
        boolean    inStock
) {
    public static CartItemDto fromItemView(CartView.ItemView view) {
        return new CartItemDto(
                view.skuId(),
                view.productName(),
                view.colorName(),
                view.sizeLabel(),
                view.imageUrl(),
                view.quantity(),
                view.unitPrice().amount(),
                view.lineTotal().amount(),
                view.availableStock(),
                view.inStock()
        );
    }
}
