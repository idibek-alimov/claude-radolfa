package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.LoyaltyLedgerEntry;

import java.time.Instant;

/**
 * Web DTO for a single loyalty ledger row, used by the ops points-history drawer.
 *
 * <p>Mapped from {@link LoyaltyLedgerEntry} via the static {@link #from} factory,
 * mirroring the {@code DiscountChangeDto} pattern.
 */
public record LoyaltyLedgerDto(
        Long    id,
        Long    userId,
        int     delta,
        String  reason,
        Long    orderId,
        Long    actorUserId,
        String  note,
        Integer remainingPoints,
        Instant expiresAt,
        int     balanceAfter,
        Instant createdAt
) {
    public static LoyaltyLedgerDto from(LoyaltyLedgerEntry e) {
        return new LoyaltyLedgerDto(
                e.id(),
                e.userId(),
                e.delta(),
                e.reason() != null ? e.reason().name() : null,
                e.orderId(),
                e.actorUserId(),
                e.note(),
                e.remainingPoints(),
                e.expiresAt(),
                e.balanceAfter(),
                e.createdAt());
    }
}
