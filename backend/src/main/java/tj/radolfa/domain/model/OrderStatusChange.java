package tj.radolfa.domain.model;

import java.time.Instant;

/**
 * An immutable, append-only record of a single order status transition.
 *
 * <p>{@code statusFrom} is {@code null} on the creation row (first ledger entry for
 * an order). {@code actorUserId} is {@code null} for system/saga-driven transitions.
 */
public record OrderStatusChange(
        Long id,
        Long orderId,
        OrderStatus statusFrom,
        OrderStatus statusTo,
        Long actorUserId,
        String reason,
        Instant occurredAt) {}
