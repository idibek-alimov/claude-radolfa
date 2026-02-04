package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Product;

import java.util.Optional;

/**
 * Out-Port: load a single product by its ERP identifier.
 */
public interface LoadProductPort {
    Optional<Product> load(String erpId);
}
