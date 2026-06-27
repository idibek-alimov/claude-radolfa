package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.LoyaltyLedgerEntry;

/**
 * Output port for persisting loyalty ledger rows.
 */
public interface SaveLoyaltyLedgerPort {

    /**
     * Appends a new ledger entry and returns the persisted row (with its generated {@code id}
     * and {@code createdAt} populated).
     */
    LoyaltyLedgerEntry append(LoyaltyLedgerEntry entry);

    /**
     * Decrements {@code remaining_points} on an existing credit lot.
     * Called during FIFO consumption (redeem / revoke / expire).
     *
     * @param lotId          the credit row to update
     * @param newRemaining   the new remaining value (≥ 0)
     */
    void updateRemaining(Long lotId, int newRemaining);
}
