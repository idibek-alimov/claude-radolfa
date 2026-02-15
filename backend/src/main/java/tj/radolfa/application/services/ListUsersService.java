package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.ListUsersUseCase;
import tj.radolfa.application.ports.out.SearchUsersPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.User;

@Service
public class ListUsersService implements ListUsersUseCase {

    private final SearchUsersPort searchUsersPort;

    public ListUsersService(SearchUsersPort searchUsersPort) {
        this.searchUsersPort = searchUsersPort;
    }

    @Override
    public PageResult<User> execute(String search, int page, int size) {
        return searchUsersPort.searchUsers(search, page, size);
    }
}
