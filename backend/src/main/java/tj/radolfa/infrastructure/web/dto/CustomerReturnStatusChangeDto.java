package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.CustomerReturnStatusChange;

import java.time.Instant;

public record CustomerReturnStatusChangeDto(
        Long id,
        Long returnId,
        String statusFrom,
        String statusTo,
        Long actorUserId,
        String reason,
        Instant occurredAt) {

    public static CustomerReturnStatusChangeDto from(CustomerReturnStatusChange c) {
        return new CustomerReturnStatusChangeDto(
                c.id(),
                c.returnId(),
                c.statusFrom() != null ? c.statusFrom().name() : null,
                c.statusTo().name(),
                c.actorUserId(),
                c.reason(),
                c.occurredAt());
    }
}
