package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.SyncLoyaltyPointsUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.User;

@Service
public class SyncLoyaltyPointsService implements SyncLoyaltyPointsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SyncLoyaltyPointsService.class);

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    public SyncLoyaltyPointsService(LoadUserPort loadUserPort, SaveUserPort saveUserPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    @Transactional
    public void execute(SyncLoyaltyCommand command) {
        var userOpt = loadUserPort.loadByPhone(command.phone());

        if (userOpt.isEmpty()) {
            LOG.warn("[LOYALTY-SYNC] No user found for phone={}. Skipping.", command.phone());
            return;
        }

        User user = userOpt.get();
        User updated = new User(
                user.id(),
                user.phone(),
                user.role(),
                user.name(),
                user.email(),
                command.points(),
                user.version());

        saveUserPort.save(updated);
        LOG.info("[LOYALTY-SYNC] Updated loyalty points for phone={}, points={}", command.phone(), command.points());
    }
}
