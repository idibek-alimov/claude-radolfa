package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.ports.in.order.CheckoutUseCase;

import java.math.BigDecimal;

public record CheckoutResponseDto(
        Long       orderId,
        String     status,
        BigDecimal subtotal,
        BigDecimal tierDiscount,
        BigDecimal pointsDiscount,
        BigDecimal total
) {
    public static CheckoutResponseDto from(CheckoutUseCase.Result result) {
        return new CheckoutResponseDto(
                result.orderId(),
                "PENDING",
                result.subtotal().amount(),
                result.tierDiscount().amount(),
                result.pointsDiscount().amount(),
                result.total().amount()
        );
    }
}
