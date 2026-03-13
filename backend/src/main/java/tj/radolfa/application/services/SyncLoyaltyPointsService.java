package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.SyncLoyaltyPointsUseCase;
import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.User;

@Service
public class SyncLoyaltyPointsService implements SyncLoyaltyPointsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SyncLoyaltyPointsService.class);

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final LoadLoyaltyTierPort loadLoyaltyTierPort;

    public SyncLoyaltyPointsService(LoadUserPort loadUserPort,
                                    SaveUserPort saveUserPort,
                                    LoadLoyaltyTierPort loadLoyaltyTierPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.loadLoyaltyTierPort = loadLoyaltyTierPort;
    }

    @Override
    @Transactional
    public void execute(SyncLoyaltyCommand command) {
        var userOpt = loadUserPort.loadByPhone(command.phone());

        if (userOpt.isEmpty()) {
            LOG.warn("[LOYALTY-SYNC] No user found for phone={}. Skipping.", command.phone());
            return;
        }

        User user = userOpt.get();
        LoyaltyTier tier = resolveTier(command.tierName(), user.loyalty().tier());

        LoyaltyProfile updatedLoyalty = new LoyaltyProfile(
                tier,
                command.points(),
                command.spendToNextTier() != null ? command.spendToNextTier() : user.loyalty().spendToNextTier(),
                command.spendToMaintainTier() != null ? command.spendToMaintainTier() : user.loyalty().spendToMaintainTier(),
                command.currentMonthSpending() != null ? command.currentMonthSpending() : user.loyalty().currentMonthSpending());

        User updated = new User(
                user.id(),
                user.phone(),
                user.role(),
                user.name(),
                user.email(),
                updatedLoyalty,
                user.enabled(),
                user.version());

        saveUserPort.save(updated);
        LOG.info("[LOYALTY-SYNC] Updated loyalty for phone={}, points={}, tier={}",
                command.phone(), command.points(), tier != null ? tier.name() : "none");
    }

    private LoyaltyTier resolveTier(String tierName, LoyaltyTier currentTier) {
        if (tierName == null || tierName.isBlank()) return currentTier;
        return loadLoyaltyTierPort.findByName(tierName).orElse(currentTier);
    }
}
