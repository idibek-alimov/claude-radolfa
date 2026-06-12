package tj.radolfa.application.ports.out;

import tj.radolfa.application.readmodel.SellerOrderItemRow;
import tj.radolfa.domain.model.PageResult;

/**
 * Output port: load a seller's own order items with server-side filtering and pagination.
 * The {@code sellerId} filter is applied at the DB level — never in-memory.
 */
public interface LoadSellerOrderItemsPort {

    /**
     * @param sellerId  the seller whose items to load (mandatory — never null at call site)
     * @param search    case-insensitive substring match on {@code product_name} / {@code sku_code};
     *                  empty string means no filter
     * @param sortBy    sort column; must be pre-validated against a whitelist in the service layer
     * @param sortDir   "ASC" or "DESC" (pre-validated)
     * @param page      1-based page number
     * @param size      page size (clamped to ≤ 100 by the service layer)
     */
    PageResult<SellerOrderItemRow> findBySellerId(Long sellerId,
                                                  String search,
                                                  String sortBy,
                                                  String sortDir,
                                                  int page,
                                                  int size);
}
