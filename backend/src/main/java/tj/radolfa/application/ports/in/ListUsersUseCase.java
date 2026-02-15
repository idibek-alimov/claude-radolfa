package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.User;

public interface ListUsersUseCase {
    PageResult<User> execute(String search, int page, int size);
}
