package tj.radolfa.domain.model;

/**
 * Describes how many points to draw from one credit lot during FIFO consumption.
 *
 * <p>Produced by {@link tj.radolfa.domain.service.LoyaltyCalculator#planConsumption}
 * and consumed by the ledger writer to emit one debit row per touched lot and to
 * update {@code remaining_points} on the source credit row.
 *
 * @param lotId        PK of the credit {@code loyalty_ledger} row to draw from
 * @param drawAmount   points drawn from this lot ({@code 1 ≤ drawAmount ≤ lot.remainingPoints})
 * @param newRemaining points left in the lot after this draw ({@code remainingPoints - drawAmount})
 */
public record LotDraw(Long lotId, int drawAmount, int newRemaining) {}
