package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Product;

/**
 * Out-Port: persist (insert or update) a product.
 */
public interface SaveProductPort {
    Product save(Product product);
}
