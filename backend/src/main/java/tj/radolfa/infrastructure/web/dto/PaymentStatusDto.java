package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.Payment;

import java.math.BigDecimal;

public record PaymentStatusDto(
        Long       paymentId,
        String     status,
        String     provider,
        BigDecimal amount
) {
    public static PaymentStatusDto from(Payment payment) {
        return new PaymentStatusDto(
                payment.id(),
                payment.status().name(),
                payment.provider(),
                payment.amount().amount()
        );
    }
}
