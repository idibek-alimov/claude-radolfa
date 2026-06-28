package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.PaymentStatusChange;

import java.time.Instant;

public record PaymentStatusChangeDto(
        Long id,
        Long paymentId,
        String statusFrom,
        String statusTo,
        Long actorUserId,
        String reason,
        Instant occurredAt) {

    public static PaymentStatusChangeDto from(PaymentStatusChange c) {
        return new PaymentStatusChangeDto(
                c.id(),
                c.paymentId(),
                c.statusFrom() != null ? c.statusFrom().name() : null,
                c.statusTo().name(),
                c.actorUserId(),
                c.reason(),
                c.occurredAt());
    }
}
