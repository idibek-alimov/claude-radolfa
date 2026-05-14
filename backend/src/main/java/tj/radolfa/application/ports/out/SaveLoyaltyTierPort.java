package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.LoyaltyTier;

import java.util.List;

public interface SaveLoyaltyTierPort {

    LoyaltyTier save(LoyaltyTier tier);

    List<LoyaltyTier> saveAll(List<LoyaltyTier> tiers);
}
