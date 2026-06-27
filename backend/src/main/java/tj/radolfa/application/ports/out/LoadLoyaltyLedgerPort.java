package tj.radolfa.application.ports.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.LoyaltyLedgerEntry;

import java.time.Instant;
import java.util.List;

/**
 * Output port for reading loyalty ledger rows.
 */
public interface LoadLoyaltyLedgerPort {

    /**
     * Returns a paginated view of all ledger movements for a user, newest first.
     * Used by the ops points-history drawer.
     */
    Page<LoyaltyLedgerEntry> findByUserId(Long userId, Pageable pageable);

    /**
     * Returns all live credit lots for a user in FIFO order (oldest {@code created_at} first).
     * A "live lot" is a credit row with {@code remaining_points > 0}.
     * Used by the FIFO consumption planner.
     */
    List<LoyaltyLedgerEntry> findLiveLots(Long userId);

    /**
     * Returns all live credit lots whose {@code expires_at} is before {@code now}.
     * Used by the daily expiry sweep.
     */
    List<LoyaltyLedgerEntry> findExpiredLots(Instant now);
}
