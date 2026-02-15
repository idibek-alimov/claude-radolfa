package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.ToggleUserStatusUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

@Service
public class ToggleUserStatusService implements ToggleUserStatusUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    public ToggleUserStatusService(LoadUserPort loadUserPort, SaveUserPort saveUserPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    @Transactional
    public User execute(Long callerId, UserRole callerRole, Long targetUserId, boolean enabled) {
        if (callerId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot change your own account status");
        }

        User target = loadUserPort.loadById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + targetUserId));

        if (target.role() == UserRole.SYSTEM) {
            throw new IllegalArgumentException("Cannot change status of SYSTEM users");
        }

        if (callerRole == UserRole.MANAGER && target.role() == UserRole.MANAGER) {
            throw new IllegalArgumentException("Managers cannot block other managers");
        }

        User updated = new User(
                target.id(),
                target.phone(),
                target.role(),
                target.name(),
                target.email(),
                target.loyaltyPoints(),
                enabled,
                target.version());

        return saveUserPort.save(updated);
    }
}
