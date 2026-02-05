package tj.radolfa.application.ports.out;

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
     * Load products marked as top-selling.
     */
    List<Product> loadTopSelling();
}
