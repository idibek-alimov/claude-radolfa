package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.ListAdminOrdersUseCase;
import tj.radolfa.application.ports.out.LoadAdminOrdersPort;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ListAdminOrdersService implements ListAdminOrdersUseCase {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("createdAt", "totalAmount", "status", "id");

    private final LoadAdminOrdersPort loadAdminOrdersPort;

    public ListAdminOrdersService(LoadAdminOrdersPort loadAdminOrdersPort) {
        this.loadAdminOrdersPort = loadAdminOrdersPort;
    }

    @Override
    public PageResult<LoadAdminOrdersPort.OrderRow> execute(String search,
                                                             OrderStatus status,
                                                             String sortBy,
                                                             String sortDir,
                                                             int page,
                                                             int size) {
        String safeSortBy  = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        String safeSortDir = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        int safePage       = Math.max(1, page);
        int safeSize       = Math.min(Math.max(1, size), 100);

        return loadAdminOrdersPort.search(search, status, safeSortBy, safeSortDir, safePage, safeSize);
    }
}
