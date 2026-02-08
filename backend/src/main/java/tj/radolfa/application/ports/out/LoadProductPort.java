package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Product;

import java.util.List;
import java.util.Optional;

/**
 * Out-Port: load products from persistence.
 */
public interface LoadProductPort {

    /**
     * Load a single product by its ERP identifier.
     */
    Optional<Product> load(String erpId);

    /**
     * Load all products.
     */
    List<Product> loadAll();

    /**
     * Load a paginated, optionally filtered page of products.
     *
     * @param page   1-based page number
     * @param limit  items per page
     * @param search nullable — SQL ILIKE on name, exact match on erpId
     * @return a framework-agnostic page of domain products
     */
    PageResult<Product> loadPage(int page, int limit, String search);

    /**
     * Load products marked as top-selling.
     */
    List<Product> loadTopSelling();
}
