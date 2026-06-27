package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.RestoreLoyaltyPointsUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyReason;
import tj.radolfa.domain.model.User;
import tj.radolfa.infrastructure.config.LoyaltyRewardProperties;

/**
 * Credits loyalty points back to a user's balance when an order is cancelled.
 *
 * <p>Counterpart to {@link RedeemLoyaltyPointsService}, which deducts points
 * pessimistically at checkout.
 *
 * <p>A fresh credit lot is appended with a new 12-month (configurable) expiry
 * rather than restoring into the original lots — this is the simplest, most
 * user-friendly approach and is intentional per the design decision in
 * {@code updates/management/03-loyalty-points-ledger.md}.
 */
@Service
@Transactional
public class RestoreLoyaltyPointsService implements RestoreLoyaltyPointsUseCase {

    private final LoadUserPort            loadUserPort;
    private final SaveUserPort            saveUserPort;
    private final LoyaltyLedgerWriter     ledgerWriter;
    private final LoyaltyRewardProperties properties;

    public RestoreLoyaltyPointsService(LoadUserPort loadUserPort,
                                       SaveUserPort saveUserPort,
                                       LoyaltyLedgerWriter ledgerWriter,
                                       LoyaltyRewardProperties properties) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.ledgerWriter = ledgerWriter;
        this.properties   = properties;
    }

    @Override
    public void execute(Long userId, int pointsToRestore) {
        if (pointsToRestore <= 0) {
            throw new IllegalArgumentException("pointsToRestore must be positive, got: " + pointsToRestore);
        }

        User user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        LoyaltyProfile profile  = user.loyalty();
        int            oldPoints = profile.points();

        ledgerWriter.credit(userId, pointsToRestore, LoyaltyReason.RESTORE,
                null, null, oldPoints, properties.pointsTtlMonths());

        LoyaltyProfile restored = new LoyaltyProfile(
                profile.tier(),
                oldPoints + pointsToRestore,
                profile.spendToNextTier(),
                profile.spendToMaintainTier(),
                profile.currentMonthSpending(),
                profile.permanent(),
                profile.lowestTierEver());

        saveUserPort.save(new User(
                user.id(), user.phone(), user.role(), user.name(),
                user.email(), restored, user.enabled(), user.version()));
    }
}
