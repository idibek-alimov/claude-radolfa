package tj.radolfa.domain.model;

import java.time.Instant;

public record DiscountChange(
        Long id, Long discountId, ChangeType changeType,
        String oldValueJson, String newValueJson,
        Long actorUserId, Instant occurredAt) {}
