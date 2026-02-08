package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Product;

/**
 * Out-Port: index (or re-index) a product into Elasticsearch.
 *
 * The real adapter that talks to an ES cluster is wired in a later phase.
 * A logging stub is active on {@code dev} and {@code test} profiles.
 */
public interface ElasticsearchProductIndexer {

    /**
     * Upsert a product document into the search index.
     *
     * @param product the domain product that was just persisted
     */
    void index(Product product);

    /**
     * Remove a product document from the search index.
     *
     * @param erpId the ERP identifier of the product to remove
     */
    void delete(String erpId);
}
