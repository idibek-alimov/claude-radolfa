package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.RevokeAwardedPointsUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.User;

/**
 * Deducts cashback points that were awarded after a payment that is now refunded.
 *
 * <p>The user's balance is floored at zero — it can never go negative as a result
 * of a revocation.
 */
@Service
@Transactional
public class RevokeAwardedPointsService implements RevokeAwardedPointsUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    public RevokeAwardedPointsService(LoadUserPort loadUserPort, SaveUserPort saveUserPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    public void execute(Long userId, int pointsToRevoke) {
        if (pointsToRevoke <= 0) {
            throw new IllegalArgumentException("pointsToRevoke must be positive, got: " + pointsToRevoke);
        }

        User user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        LoyaltyProfile profile = user.loyalty();
        int newBalance = Math.max(0, profile.points() - pointsToRevoke);

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
