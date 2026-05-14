package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.ToggleLoyaltyPermanentUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.User;

@Service
@Transactional
public class ToggleLoyaltyPermanentService implements ToggleLoyaltyPermanentUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    public ToggleLoyaltyPermanentService(LoadUserPort loadUserPort, SaveUserPort saveUserPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    public User execute(Long userId, boolean permanent) {
        User user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        LoyaltyProfile current = user.loyalty();
        LoyaltyProfile updated = new LoyaltyProfile(
                current.tier(),
                current.points(),
                current.spendToNextTier(),
                current.spendToMaintainTier(),
                current.currentMonthSpending(),
                permanent,
                current.lowestTierEver());

        return saveUserPort.save(new User(
                user.id(), user.phone(), user.role(), user.name(),
                user.email(), updated, user.enabled(), user.version()));
    }
}
