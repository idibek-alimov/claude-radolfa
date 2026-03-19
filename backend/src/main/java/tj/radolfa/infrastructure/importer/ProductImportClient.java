package tj.radolfa.infrastructure.importer;

import java.util.List;

/**
 * Out-facing port toward the external product catalogue.
 *
 * Abstracted so that the batch reader does not hard-code HTTP concerns.
 * The real HTTP implementation is wired for production;
 * {@link ProductImportClientStub} serves {@code dev} and {@code test} profiles.
 */
public interface ProductImportClient {

    /**
     * Fetch one page of products from the external catalogue.
     *
     * @param page  1-based page number
     * @param limit page size (max 100)
     * @return list of raw product snapshots; an empty list signals
     *         that there are no more pages
     */
    List<ImportedProductSnapshot> fetchPage(int page, int limit);
}
