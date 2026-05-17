package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.util.List;

public interface ListUsersUseCase {
    PageResult<User> execute(String search, int page, int size);
    PageResult<User> execute(String search, List<UserRole> roles, int page, int size);
}
