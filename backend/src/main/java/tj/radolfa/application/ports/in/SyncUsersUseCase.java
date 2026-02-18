package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.UserRole;

import java.util.List;

/**
 * Use case for syncing user profiles from ERPNext.
 * Performs upsert by phone number.
 */
public interface SyncUsersUseCase {

    record SyncUserCommand(
            String phone,
            String name,
            String email,
            UserRole role,
            Boolean enabled,
            Integer loyaltyPoints) {
    }

    record SyncResult(int synced, int errors) {}

    void executeOne(SyncUserCommand command);

    SyncResult executeBatch(List<SyncUserCommand> commands);
}
