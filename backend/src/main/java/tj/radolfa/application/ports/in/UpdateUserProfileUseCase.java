package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.User;

public interface UpdateUserProfileUseCase {
    User execute(Long userId, String name, String email);
}
