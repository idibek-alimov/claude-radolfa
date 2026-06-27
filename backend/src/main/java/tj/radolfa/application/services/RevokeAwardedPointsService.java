package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.RevokeAwardedPointsUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyReason;
import tj.radolfa.domain.model.User;

/**
 * Deducts cashback points that were awarded after a payment that is now refunded.
 *
 * <p>The user's balance is floored at zero — it can never go negative as a result
 * of a revocation. If the available balance is less than the amount to revoke,
 * only the available balance is consumed.
 *
 * <p>Each lot consumed via FIFO produces one {@code REVOKE} debit row in the ledger.
 * If the balance is already zero, no ledger row is written.
 */
@Service
@Transactional
public class RevokeAwardedPointsService implements RevokeAwardedPointsUseCase {

    private final LoadUserPort        loadUserPort;
    private final SaveUserPort        saveUserPort;
    private final LoyaltyLedgerWriter ledgerWriter;

    public RevokeAwardedPointsService(LoadUserPort loadUserPort,
                                      SaveUserPort saveUserPort,
                                      LoyaltyLedgerWriter ledgerWriter) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.ledgerWriter = ledgerWriter;
    }

    @Override
    public void execute(Long userId, int pointsToRevoke) {
        if (pointsToRevoke <= 0) {
            throw new IllegalArgumentException("pointsToRevoke must be positive, got: " + pointsToRevoke);
        }

        User user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        LoyaltyProfile profile   = user.loyalty();
        int            oldPoints  = profile.points();
        // Clamp to available balance so planConsumption never over-draws
        int            toConsume  = Math.min(pointsToRevoke, oldPoints);

        if (toConsume > 0) {
            ledgerWriter.debit(userId, toConsume, LoyaltyReason.REVOKE,
                    null, null, oldPoints);
        }

        int newBalance = oldPoints - toConsume; // == Math.max(0, oldPoints - pointsToRevoke)
        LoyaltyProfile revoked = new LoyaltyProfile(
                profile.tier(),
                newBalance,
                profile.spendToNextTier(),
                profile.spendToMaintainTier(),
                profile.currentMonthSpending(),
                profile.permanent(),
                profile.lowestTierEver());

        saveUserPort.save(new User(
                user.id(), user.phone(), user.role(), user.name(),
                user.email(), revoked, user.enabled(), user.version()));
    }
}
