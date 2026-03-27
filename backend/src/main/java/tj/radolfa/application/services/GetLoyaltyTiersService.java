package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.GetLoyaltyTiersUseCase;
import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.domain.model.LoyaltyTier;

import java.util.List;

@Service
public class GetLoyaltyTiersService implements GetLoyaltyTiersUseCase {

    private final LoadLoyaltyTierPort loadLoyaltyTierPort;

    public GetLoyaltyTiersService(LoadLoyaltyTierPort loadLoyaltyTierPort) {
        this.loadLoyaltyTierPort = loadLoyaltyTierPort;
    }

    @Override
    public List<LoyaltyTier> findAll() {
        return loadLoyaltyTierPort.findAll();
    }
}
