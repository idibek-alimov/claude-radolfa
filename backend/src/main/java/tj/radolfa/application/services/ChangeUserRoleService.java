package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.ChangeUserRoleUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

@Service
public class ChangeUserRoleService implements ChangeUserRoleUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    public ChangeUserRoleService(LoadUserPort loadUserPort, SaveUserPort saveUserPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    @Transactional
    public User execute(Long userId, UserRole newRole) {
        User user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        User updated = new User(
                user.id(),
                user.phone(),
                newRole,
                user.name(),
                user.email(),
                user.loyalty(),
                user.enabled(),
                user.version());

        return saveUserPort.save(updated);
    }
}
