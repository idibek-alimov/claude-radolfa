package tj.radolfa.domain.model;

import java.time.Instant;

/**
 * Immutable, append-only record of a single payment status transition.
 *
 * <p>{@code statusFrom} is {@code null} on the creation row (first ledger entry for
 * a payment). {@code actorUserId} is {@code null} for system/saga/webhook-driven transitions.
 */
public record PaymentStatusChange(
        Long id,
        Long paymentId,
        PaymentStatus statusFrom,
        PaymentStatus statusTo,
        Long actorUserId,
        String reason,
        Instant occurredAt) {}
