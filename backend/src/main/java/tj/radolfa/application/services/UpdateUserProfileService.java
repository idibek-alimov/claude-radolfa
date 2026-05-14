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

        String trimmedName = name != null ? name.trim() : null;
        String trimmedEmail = email != null ? email.trim().toLowerCase() : null;

        // Treat blank strings as null
        if (trimmedName != null && trimmedName.isEmpty()) trimmedName = null;
        if (trimmedEmail != null && trimmedEmail.isEmpty()) trimmedEmail = null;

        User updatedUser = new User(
                user.id(),
                user.phone(),
                user.role(),
                trimmedName,
                trimmedEmail,
                user.loyalty(),
                user.enabled(),
                user.version());

        return saveUserPort.save(updatedUser);
    }
}
