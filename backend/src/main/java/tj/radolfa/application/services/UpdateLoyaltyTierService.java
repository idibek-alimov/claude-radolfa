package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.UpdateLoyaltyTierUseCase;
import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.application.ports.out.SaveLoyaltyTierPort;
import tj.radolfa.domain.model.LoyaltyTier;

import java.util.NoSuchElementException;
import java.util.regex.Pattern;

@Service
public class UpdateLoyaltyTierService implements UpdateLoyaltyTierUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateLoyaltyTierService.class);
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final LoadLoyaltyTierPort loadLoyaltyTierPort;
    private final SaveLoyaltyTierPort saveLoyaltyTierPort;

    public UpdateLoyaltyTierService(LoadLoyaltyTierPort loadLoyaltyTierPort,
                                    SaveLoyaltyTierPort saveLoyaltyTierPort) {
        this.loadLoyaltyTierPort = loadLoyaltyTierPort;
        this.saveLoyaltyTierPort = saveLoyaltyTierPort;
    }

    @Override
    @Transactional
    public void updateColor(UpdateTierColorCommand command) {
        if (!HEX_COLOR.matcher(command.color()).matches()) {
            throw new IllegalArgumentException(
                    "Invalid hex color format: " + command.color() + ". Expected format: #RRGGBB");
        }

        LoyaltyTier tier = loadLoyaltyTierPort.findById(command.tierId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Loyalty tier not found with id: " + command.tierId()));

        LoyaltyTier updated = new LoyaltyTier(
                tier.id(),
                tier.name(),
                tier.discountPercentage(),
                tier.cashbackPercentage(),
                tier.minSpendRequirement(),
                tier.displayOrder(),
                command.color());

        saveLoyaltyTierPort.save(updated);
        LOG.info("[TIER-UPDATE] Updated color for tier={} to {}", tier.name(), command.color());
    }
}
