package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.SyncUsersUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.util.List;

@Service
public class SyncUsersService implements SyncUsersUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SyncUsersService.class);

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    public SyncUsersService(LoadUserPort loadUserPort, SaveUserPort saveUserPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    @Transactional
    public void executeOne(SyncUserCommand command) {
        upsert(command);
    }

    @Override
    @Transactional
    public SyncResult executeBatch(List<SyncUserCommand> commands) {
        int synced = 0;
        int errors = 0;

        for (SyncUserCommand command : commands) {
            try {
                upsert(command);
                synced++;
            } catch (Exception ex) {
                LOG.error("[USER-SYNC] Failed to sync phone={}: {}", command.phone(), ex.getMessage(), ex);
                errors++;
            }
        }

        return new SyncResult(synced, errors);
    }

    private void upsert(SyncUserCommand command) {
        PhoneNumber phone = new PhoneNumber(command.phone());
        String email = (command.email() != null && !command.email().isBlank()) ? command.email() : null;
        var existing = loadUserPort.loadByPhone(phone.value());

        if (existing.isPresent()) {
            User user = existing.get();
            int points = command.loyaltyPoints() != null ? command.loyaltyPoints() : user.loyalty().points();
            LoyaltyProfile loyalty = new LoyaltyProfile(
                    user.loyalty().tier(),
                    points,
                    user.loyalty().spendToNextTier(),
                    user.loyalty().spendToMaintainTier(),
                    user.loyalty().currentMonthSpending());

            User updated = new User(
                    user.id(),
                    phone,
                    command.role() != null ? command.role() : user.role(),
                    command.name() != null ? command.name() : user.name(),
                    email != null ? email : user.email(),
                    loyalty,
                    command.enabled() != null ? command.enabled() : user.enabled(),
                    user.version());
            saveUserPort.save(updated);
            LOG.info("[USER-SYNC] Updated user phone={}", phone.value());
        } else {
            int points = command.loyaltyPoints() != null ? command.loyaltyPoints() : 0;
            LoyaltyProfile loyalty = new LoyaltyProfile(null, points, null, null, null);

            User newUser = new User(
                    null,
                    phone,
                    command.role() != null ? command.role() : UserRole.USER,
                    command.name(),
                    email,
                    loyalty,
                    command.enabled() != null ? command.enabled() : true,
                    null);
            saveUserPort.save(newUser);
            LOG.info("[USER-SYNC] Created new user phone={}", phone.value());
        }
    }
}
