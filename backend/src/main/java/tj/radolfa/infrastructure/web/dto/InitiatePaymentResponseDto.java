package tj.radolfa.infrastructure.web.dto;

public record InitiatePaymentResponseDto(
        Long   paymentId,
        String redirectUrl
) {}
