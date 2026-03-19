package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.sync.SyncLoyaltyTiersUseCase;
import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.application.ports.out.SaveLoyaltyTierPort;
import tj.radolfa.domain.model.LoyaltyTier;

import java.util.List;

@Service
public class SyncLoyaltyTiersService implements SyncLoyaltyTiersUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SyncLoyaltyTiersService.class);

    private final LoadLoyaltyTierPort loadLoyaltyTierPort;
    private final SaveLoyaltyTierPort saveLoyaltyTierPort;

    public SyncLoyaltyTiersService(LoadLoyaltyTierPort loadLoyaltyTierPort,
                                   SaveLoyaltyTierPort saveLoyaltyTierPort) {
        this.loadLoyaltyTierPort = loadLoyaltyTierPort;
        this.saveLoyaltyTierPort = saveLoyaltyTierPort;
    }

    @Override
    @Transactional
    public void execute(List<SyncTierCommand> commands) {
        for (SyncTierCommand command : commands) {
            upsert(command);
        }
        LOG.info("[TIER-SYNC] Synced {} loyalty tiers", commands.size());
    }

    private void upsert(SyncTierCommand command) {
        var existing = loadLoyaltyTierPort.findByName(command.name());

        if (existing.isPresent()) {
            LoyaltyTier tier = existing.get();
            LoyaltyTier updated = new LoyaltyTier(
                    tier.id(),
                    command.name(),
                    command.discountPercentage(),
                    command.cashbackPercentage(),
                    command.minSpendRequirement(),
                    command.displayOrder(),
                    command.color() != null ? command.color() : tier.color());
            saveLoyaltyTierPort.save(updated);
            LOG.info("[TIER-SYNC] Updated tier={}", command.name());
        } else {
            LoyaltyTier newTier = new LoyaltyTier(
                    null,
                    command.name(),
                    command.discountPercentage(),
                    command.cashbackPercentage(),
                    command.minSpendRequirement(),
                    command.displayOrder(),
                    command.color() != null ? command.color() : "#6366F1");
            saveLoyaltyTierPort.save(newTier);
            LOG.info("[TIER-SYNC] Created tier={}", command.name());
        }
    }
}
