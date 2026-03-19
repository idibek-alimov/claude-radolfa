package tj.radolfa.application.ports.in.sync;

import java.math.BigDecimal;

/**
 * Use case for syncing loyalty points from the external source.
 * Lookup is by phone number (unique per user).
 */
public interface SyncLoyaltyPointsUseCase {

    record SyncLoyaltyCommand(
            String phone,
            int points,
            String tierName,
            BigDecimal spendToNextTier,
            BigDecimal spendToMaintainTier,
            BigDecimal currentMonthSpending) {
    }

    void execute(SyncLoyaltyCommand command);
}
