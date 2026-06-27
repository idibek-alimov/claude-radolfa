package tj.radolfa.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.GetLoyaltyLedgerUseCase;
import tj.radolfa.application.ports.out.LoadLoyaltyLedgerPort;
import tj.radolfa.domain.model.LoyaltyLedgerEntry;

/**
 * Returns a paginated view of a user's loyalty ledger for the ops points-history drawer.
 *
 * <p>This is a thin read-only delegate to {@link LoadLoyaltyLedgerPort#findByUserId},
 * mirroring the structure of {@code GetDiscountChangeLogService}.
 */
@Service
@Transactional(readOnly = true)
public class GetLoyaltyLedgerService implements GetLoyaltyLedgerUseCase {

    private final LoadLoyaltyLedgerPort loadLoyaltyLedgerPort;

    public GetLoyaltyLedgerService(LoadLoyaltyLedgerPort loadLoyaltyLedgerPort) {
        this.loadLoyaltyLedgerPort = loadLoyaltyLedgerPort;
    }

    @Override
    public Page<LoyaltyLedgerEntry> execute(Long userId, Pageable pageable) {
        return loadLoyaltyLedgerPort.findByUserId(userId, pageable);
    }
}
