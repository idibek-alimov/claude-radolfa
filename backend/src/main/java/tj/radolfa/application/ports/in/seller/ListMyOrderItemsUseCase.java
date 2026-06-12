package tj.radolfa.application.ports.in.seller;

import tj.radolfa.application.readmodel.SellerOrderItemRow;
import tj.radolfa.domain.model.PageResult;

/**
 * Input port: a seller lists the order items attributed to them.
 * Read-only — Radolfa fulfils all orders; sellers have no fulfilment controls.
 */
public interface ListMyOrderItemsUseCase {

    /**
     * @param sellerId  resolved from the authenticated seller's profile — never from the request body
     * @param search    substring search on product name / sku code (empty = no filter)
     * @param sortBy    sort field; unknown values fall back to {@code "createdAt"} in the service
     * @param sortDir   "ASC" or "DESC"; invalid values fall back to "DESC"
     * @param page      1-based page number (clamped to ≥ 1)
     * @param size      page size (clamped to 1–100)
     */
    PageResult<SellerOrderItemRow> execute(Long sellerId,
                                           String search,
                                           String sortBy,
                                           String sortDir,
                                           int page,
                                           int size);
}
