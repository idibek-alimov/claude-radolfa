package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyLedgerEntry;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.User;

import java.util.List;

/**
 * Expires all past-due credit lots for a single user in one transaction.
 *
 * <p>Extracted into its own bean so that {@code @Transactional} is applied
 * through the Spring proxy (self-invocation from {@link ExpireLoyaltyPointsService}
 * would bypass the proxy). Mirrors {@link UserTierEvaluatorService}.
 */
@Service
@Transactional
public class UserPointsExpiryService {

    private final LoadUserPort        loadUserPort;
    private final SaveUserPort        saveUserPort;
    private final LoyaltyLedgerWriter ledgerWriter;

    public UserPointsExpiryService(LoadUserPort loadUserPort,
                                   SaveUserPort saveUserPort,
                                   LoyaltyLedgerWriter ledgerWriter) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.ledgerWriter = ledgerWriter;
    }

    /**
     * Expires the given lots for one user: writes one {@code EXPIRE} debit row per lot,
     * zeroes each lot's {@code remaining_points}, and updates the cached balance once.
     *
     * @param userId      the user whose lots are expiring
     * @param expiredLots non-empty list of credit lots past their {@code expires_at}
     */
    public void expire(Long userId, List<LoyaltyLedgerEntry> expiredLots) {
        User user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        LoyaltyProfile profile = user.loyalty();
        int balance = profile.points();

        for (LoyaltyLedgerEntry lot : expiredLots) {
            balance = ledgerWriter.expireLot(lot, balance);
        }

        LoyaltyProfile updated = new LoyaltyProfile(
                profile.tier(),
                balance,
                profile.spendToNextTier(),
                profile.spendToMaintainTier(),
                profile.currentMonthSpending(),
                profile.permanent(),
                profile.lowestTierEver());

        saveUserPort.save(new User(
                user.id(), user.phone(), user.role(), user.name(),
                user.email(), updated, user.enabled(), user.version()));
    }
}
