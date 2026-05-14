package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.AssignUserTierUseCase;
import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.User;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class AssignUserTierService implements AssignUserTierUseCase {

    private final LoadUserPort          loadUserPort;
    private final LoadLoyaltyTierPort   loadLoyaltyTierPort;
    private final SaveUserPort          saveUserPort;
    private final LoyaltySpendCalculator loyaltySpendCalculator;

    public AssignUserTierService(LoadUserPort loadUserPort,
                                 LoadLoyaltyTierPort loadLoyaltyTierPort,
                                 SaveUserPort saveUserPort,
                                 LoyaltySpendCalculator loyaltySpendCalculator) {
        this.loadUserPort          = loadUserPort;
        this.loadLoyaltyTierPort   = loadLoyaltyTierPort;
        this.saveUserPort          = saveUserPort;
        this.loyaltySpendCalculator = loyaltySpendCalculator;
    }

    @Override
    public User execute(Command command) {
        User user = loadUserPort.loadById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + command.userId()));

        LoyaltyTier tier = loadLoyaltyTierPort.findById(command.tierId())
                .orElseThrow(() -> new IllegalArgumentException("Tier not found: " + command.tierId()));

        List<LoyaltyTier> allTiers = loadLoyaltyTierPort.findAll();

        LoyaltyProfile current = user.loyalty();

        // Determine floor tier: entry-level (min displayOrder). Set once, never cleared.
        LoyaltyTier lowestTierEver = current.lowestTierEver();
        if (lowestTierEver == null) {
            lowestTierEver = allTiers.stream()
                    .min(Comparator.comparingInt(LoyaltyTier::displayOrder))
                    .orElse(tier);
        }

        BigDecimal spendToNext = loyaltySpendCalculator.computeSpendToNextTier(
                tier, null, current.currentMonthSpending());

        LoyaltyProfile updated = new LoyaltyProfile(
                tier,
                current.points(),
                spendToNext,
                current.spendToMaintainTier(),
                current.currentMonthSpending(),
                current.permanent(),
                lowestTierEver);

        return saveUserPort.save(new User(
                user.id(), user.phone(), user.role(), user.name(),
                user.email(), updated, user.enabled(), user.version()));
    }
}
