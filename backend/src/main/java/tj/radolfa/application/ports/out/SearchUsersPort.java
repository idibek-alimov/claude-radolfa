package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.util.Collection;

public interface SearchUsersPort {
    PageResult<User> searchUsers(String query, int page, int size);
    default PageResult<User> searchUsersByRoles(String query, Collection<UserRole> roles, int page, int size) {
        return searchUsers(query, page, size);
    }
}
