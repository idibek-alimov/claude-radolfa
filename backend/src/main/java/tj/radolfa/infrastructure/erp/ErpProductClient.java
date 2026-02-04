package tj.radolfa.infrastructure.erp;

import java.util.List;

/**
 * Out-facing port toward ERPNext.
 *
 * Abstracted so that the batch reader does not hard-code HTTP concerns.
 * The real HTTP/Feign implementation is wired in a later phase;
 * {@link ErpProductClientStub} serves {@code dev} and {@code test} profiles.
 */
public interface ErpProductClient {

    /**
     * Fetch one page of products from ERPNext.
     *
     * @param page  1-based page number
     * @param limit page size (max 100)
     * @return list of raw ERP product snapshots; an empty list signals
     *         that there are no more pages
     */
    List<ErpProductSnapshot> fetchPage(int page, int limit);
}
