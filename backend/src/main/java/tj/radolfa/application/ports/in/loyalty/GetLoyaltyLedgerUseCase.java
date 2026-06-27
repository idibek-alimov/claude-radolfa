package tj.radolfa.application.ports.in.loyalty;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.LoyaltyLedgerEntry;

/**
 * In-Port: retrieve a paginated, sorted view of a user's loyalty ledger history.
 *
 * <p>Consumed by the ops points-history drawer (MANAGER + ADMIN).
 * Server-side paging and sorting are applied at the persistence layer;
 * callers must never filter or sort the returned {@code content[]} client-side.
 */
public interface GetLoyaltyLedgerUseCase {

    /**
     * @param userId   the user whose ledger to read
     * @param pageable paging + sort parameters (already sanitised / whitelisted by the controller)
     * @return a page of ledger entries, sorted and paged server-side
     */
    Page<LoyaltyLedgerEntry> execute(Long userId, Pageable pageable);
}
