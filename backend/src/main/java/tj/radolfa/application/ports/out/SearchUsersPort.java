package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.User;

public interface SearchUsersPort {
    PageResult<User> searchUsers(String query, int page, int size);
}
