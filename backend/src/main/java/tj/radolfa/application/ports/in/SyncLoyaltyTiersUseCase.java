package tj.radolfa.application.ports.in;

import java.math.BigDecimal;
import java.util.List;

public interface SyncLoyaltyTiersUseCase {

    record SyncTierCommand(
            String name,
            BigDecimal discountPercentage,
            BigDecimal cashbackPercentage,
            BigDecimal minSpendRequirement,
            int displayOrder) {
    }

    void execute(List<SyncTierCommand> commands);
}
