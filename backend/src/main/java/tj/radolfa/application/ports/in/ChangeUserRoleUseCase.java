package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

public interface ChangeUserRoleUseCase {
    User execute(Long userId, UserRole newRole);
}
