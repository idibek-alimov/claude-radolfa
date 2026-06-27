package tj.radolfa.domain.model;

import java.time.Instant;

/**
 * Immutable representation of one row in the {@code loyalty_ledger} table.
 *
 * <h3>Credit rows ({@code delta > 0})</h3>
 * Reasons: {@link LoyaltyReason#EARN_CASHBACK}, {@link LoyaltyReason#REVIEW_BONUS},
 * {@link LoyaltyReason#RESTORE}, positive {@link LoyaltyReason#MANUAL_ADJUSTMENT},
 * {@link LoyaltyReason#OPENING_BALANCE}.
 * These rows carry {@code remainingPoints} (decremented when consumed) and
 * {@code expiresAt} ({@code null} means the lot never expires).
 *
 * <h3>Debit rows ({@code delta < 0})</h3>
 * Reasons: {@link LoyaltyReason#REDEEM}, {@link LoyaltyReason#REVOKE},
 * {@link LoyaltyReason#EXPIRE}, negative {@link LoyaltyReason#MANUAL_ADJUSTMENT}.
 * Each debit row links to the credit row it drew from via {@code sourceLotId}.
 * A single user operation may produce <em>one debit row per lot consumed</em>
 * (FIFO: oldest live lot first).
 *
 * <h3>Balance invariant</h3>
 * {@code SUM(delta) == SUM(remainingPoints WHERE delta > 0) == users.loyalty_points}
 */
public record LoyaltyLedgerEntry(

        /** Database PK; {@code null} for unsaved entries. */
        Long id,

        /** The user this movement belongs to. */
        Long userId,

        /** Signed points change: positive = credit, negative = debit. */
        int delta,

        /** Business reason for this movement. */
        LoyaltyReason reason,

        /** Order that triggered this movement; may be {@code null}. */
        Long orderId,

        /** ADMIN user who initiated a manual adjustment; {@code null} for system movements. */
        Long actorUserId,

        /** For debit rows: the credit lot this draw came from; {@code null} for credit rows. */
        Long sourceLotId,

        /**
         * For credit rows: points still available in this lot.
         * Starts equal to {@code delta}; decremented to 0 when fully consumed or expired.
         * {@code null} for debit rows.
         */
        Integer remainingPoints,

        /**
         * For credit rows: when this lot expires ({@code null} = never).
         * {@code null} for debit rows.
         */
        Instant expiresAt,

        /** Snapshot of the user's total balance immediately after this movement. */
        int balanceAfter,

        /** When this row was created. */
        Instant createdAt
) {}
