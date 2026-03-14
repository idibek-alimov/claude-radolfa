package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.LoyaltyTier;

import java.util.List;
import java.util.Optional;

public interface LoadLoyaltyTierPort {

    Optional<LoyaltyTier> findById(Long id);

    Optional<LoyaltyTier> findByName(String name);

    List<LoyaltyTier> findAll();
}
