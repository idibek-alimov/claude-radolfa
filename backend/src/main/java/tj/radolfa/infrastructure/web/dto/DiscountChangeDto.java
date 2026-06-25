package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.DiscountChange;

import java.time.Instant;

public record DiscountChangeDto(
        Long id, Long discountId, String changeType,
        String oldValue, String newValue,
        Long actorUserId, Instant occurredAt) {

    public static DiscountChangeDto from(DiscountChange c) {
        return new DiscountChangeDto(c.id(), c.discountId(),
                c.changeType() != null ? c.changeType().name() : null,
                c.oldValueJson(), c.newValueJson(), c.actorUserId(), c.occurredAt());
    }
}
