package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.LoyaltyTier;

import java.util.List;

public interface GetLoyaltyTiersUseCase {
    List<LoyaltyTier> findAll();
}
