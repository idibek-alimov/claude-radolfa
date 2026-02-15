package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

public interface ToggleUserStatusUseCase {
    User execute(Long callerId, UserRole callerRole, Long targetUserId, boolean enabled);
}
