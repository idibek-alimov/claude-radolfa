package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Product;

import java.math.BigDecimal;

/**
 * In-Port: synchronise the ERP-locked fields for a single product.
 *
 * Called exclusively by the SYSTEM role (ERP sync job).
 */
public interface SyncErpProductUseCase {

    /**
     * If a product with the given erpId already exists, its locked fields
     * are overwritten via {@code Product.enrichWithErpData(...)}.
     * If no product exists yet, a new skeleton record is created.
     *
     * @return the product after the sync merge
     */
    Product execute(String erpId, String name, BigDecimal price, Integer stock);
}
