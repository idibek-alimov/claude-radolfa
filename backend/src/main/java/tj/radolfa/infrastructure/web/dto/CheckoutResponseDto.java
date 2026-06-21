package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.ports.in.order.CheckoutUseCase;

import java.math.BigDecimal;

public record CheckoutResponseDto(
        Long       orderId,
        String     status,
        BigDecimal subtotal,
        BigDecimal tierDiscount,
        BigDecimal pointsDiscount,
        BigDecimal total,
        String     paymentMethod,
        BigDecimal handlingFee
) {
    public static CheckoutResponseDto from(CheckoutUseCase.Result result) {
        return new CheckoutResponseDto(
                result.orderId(),
                result.status().name(),
                result.subtotal().amount(),
                result.tierDiscount().amount(),
                result.pointsDiscount().amount(),
                result.total().amount(),
                result.paymentMethod().name(),
                result.handlingFee().amount()
        );
    }
}
