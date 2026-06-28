package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.OrderStatusChange;

import java.time.Instant;

public record OrderStatusChangeDto(
        Long id,
        Long orderId,
        String statusFrom,
        String statusTo,
        Long actorUserId,
        String reason,
        Instant occurredAt) {

    public static OrderStatusChangeDto from(OrderStatusChange c) {
        return new OrderStatusChangeDto(
                c.id(),
                c.orderId(),
                c.statusFrom() != null ? c.statusFrom().name() : null,
                c.statusTo().name(),
                c.actorUserId(),
                c.reason(),
                c.occurredAt());
    }
}
