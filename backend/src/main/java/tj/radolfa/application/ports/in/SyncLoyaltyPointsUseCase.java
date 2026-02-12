package tj.radolfa.application.ports.in;

/**
 * Use case for syncing loyalty points from ERPNext.
 * Lookup is by phone number (unique per user).
 */
public interface SyncLoyaltyPointsUseCase {

    record SyncLoyaltyCommand(String phone, int points) {}

    void execute(SyncLoyaltyCommand command);
}
