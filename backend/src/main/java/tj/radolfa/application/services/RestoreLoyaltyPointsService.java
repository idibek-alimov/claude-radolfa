package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.RestoreLoyaltyPointsUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.User;

/**
 * Credits loyalty points back to a user's balance when an order is cancelled.
 *
 * <p>Counterpart to {@link RedeemLoyaltyPointsService}, which deducts points
 * pessimistically at checkout.
 */
@Service
@Transactional
public class RestoreLoyaltyPointsService implements RestoreLoyaltyPointsUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    public RestoreLoyaltyPointsService(LoadUserPort loadUserPort, SaveUserPort saveUserPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    public void execute(Long userId, int pointsToRestore) {
        if (pointsToRestore <= 0) {
            throw new IllegalArgumentException("pointsToRestore must be positive, got: " + pointsToRestore);
        }

        User user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        LoyaltyProfile profile = user.loyalty();
        LoyaltyProfile restored = new LoyaltyProfile(
                profile.tier(),
                profile.points() + pointsToRestore,
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
