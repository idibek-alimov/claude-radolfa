package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.ListUsersUseCase;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.SearchUsersPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Pickpoint;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ListUsersService implements ListUsersUseCase {

    private final SearchUsersPort searchUsersPort;
    private final LoadPickpointPort loadPickpointPort;

    public ListUsersService(SearchUsersPort searchUsersPort, LoadPickpointPort loadPickpointPort) {
        this.searchUsersPort = searchUsersPort;
        this.loadPickpointPort = loadPickpointPort;
    }

    @Override
    public PageResult<User> execute(String search, int page, int size) {
        return execute(search, null, page, size);
    }

    @Override
    public PageResult<User> execute(String search, List<UserRole> roles, int page, int size) {
        PageResult<User> result = (roles == null || roles.isEmpty())
                ? searchUsersPort.searchUsers(search, page, size)
                : searchUsersPort.searchUsersByRoles(search, roles, page, size);

        // Batch-load pickpoint names for any PICKPOINT_STAFF users on this page
        Set<Long> pickpointIds = result.content().stream()
                .filter(u -> u.pickpointId() != null)
                .map(User::pickpointId)
                .collect(Collectors.toSet());

        if (pickpointIds.isEmpty()) {
            return result;
        }

        Map<Long, String> nameById = loadPickpointPort.findAllByIds(pickpointIds).stream()
                .collect(Collectors.toMap(Pickpoint::id, Pickpoint::name));

        // Re-wrap with enriched page — users are immutable records so we swap pickpointName
        // via the UserDto layer (no mutation needed here; name map is passed to controller)
        // Store the map in a thread-local-free way by returning an enriched PageResult subtype.
        // Since User is a plain record without pickpointName, enrichment happens at the DTO layer.
        // We attach the map as a wrapper so the controller can access it.
        return new EnrichedPageResult<>(result, nameById);
    }

    /** Carries the resolved pickpoint names alongside the page so UserController can use them. */
    public static class EnrichedPageResult<T> extends PageResult<T> {
        public final Map<Long, String> pickpointNames;

        public EnrichedPageResult(PageResult<T> base, Map<Long, String> pickpointNames) {
            super(base.content(), base.totalElements(), base.number(), base.size(), base.last());
            this.pickpointNames = pickpointNames;
        }
    }
}
