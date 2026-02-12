package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.UpdateUserProfileUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.User;

@Service
public class UpdateUserProfileService implements UpdateUserProfileUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    public UpdateUserProfileService(LoadUserPort loadUserPort, SaveUserPort saveUserPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    @Transactional
    public User execute(Long userId, String name, String email) {
        User user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        User updatedUser = new User(
                user.id(),
                user.phone(),
                user.role(),
                name,
                email,
                user.loyaltyPoints(),
                user.version());

        return saveUserPort.save(updatedUser);
    }
}
