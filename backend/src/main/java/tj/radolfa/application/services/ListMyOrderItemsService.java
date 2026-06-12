package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.seller.ListMyOrderItemsUseCase;
import tj.radolfa.application.ports.out.LoadSellerOrderItemsPort;
import tj.radolfa.application.readmodel.SellerOrderItemRow;
import tj.radolfa.domain.model.PageResult;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ListMyOrderItemsService implements ListMyOrderItemsUseCase {

    /**
     * Sort columns mapped to native SQL aliases used in {@code SellerOrderItemQueryRepository}.
     * "createdAt" maps to {@code o.created_at} via the JPA {@code Pageable} sort convention;
     * the native query aliases the column as {@code orderCreatedAt} and the Pageable sort
     * field name must match the projection getter camelCase without the "get" prefix, but for
     * native queries Spring Data passes the column name directly — so we use the DB column name.
     */
    private static final Set<String> SORTABLE_FIELDS = Set.of("orderCreatedAt", "orderStatus");

    private final LoadSellerOrderItemsPort loadSellerOrderItemsPort;

    public ListMyOrderItemsService(LoadSellerOrderItemsPort loadSellerOrderItemsPort) {
        this.loadSellerOrderItemsPort = loadSellerOrderItemsPort;
    }

    @Override
    public PageResult<SellerOrderItemRow> execute(Long sellerId,
                                                   String search,
                                                   String sortBy,
                                                   String sortDir,
                                                   int page,
                                                   int size) {
        String safeSortBy  = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "orderCreatedAt";
        String safeSortDir = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        int    safePage    = Math.max(1, page);
        int    safeSize    = Math.min(Math.max(1, size), 100);

        return loadSellerOrderItemsPort.findBySellerId(sellerId, search, safeSortBy, safeSortDir, safePage, safeSize);
    }
}
