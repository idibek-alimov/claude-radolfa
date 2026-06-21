package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.PaymentMethod;
import tj.radolfa.infrastructure.config.CheckoutProperties;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public record CheckoutOptionsResponseDto(
        BigDecimal     codHandlingFee,
        List<String>   paymentMethods
) {
    public static CheckoutOptionsResponseDto from(CheckoutProperties properties) {
        return new CheckoutOptionsResponseDto(
                properties.codHandlingFee(),
                Arrays.stream(PaymentMethod.values()).map(Enum::name).toList()
        );
    }
}
