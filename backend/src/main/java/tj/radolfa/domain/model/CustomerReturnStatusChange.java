package tj.radolfa.domain.model;

import java.time.Instant;

/**
 * Immutable, append-only record of a single customer-return status transition.
 *
 * <p>{@code statusFrom} is {@code null} on the creation row (first ledger entry for
 * a return). {@code actorUserId} is {@code null} for system-driven transitions.
 */
public record CustomerReturnStatusChange(
        Long id,
        Long returnId,
        CustomerReturnStatus statusFrom,
        CustomerReturnStatus statusTo,
        Long actorUserId,
        String reason,
        Instant occurredAt) {}
