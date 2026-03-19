package tj.radolfa.application.ports.in.product;

import tj.radolfa.domain.model.Money;

/**
 * In-Port: set the price on a specific SKU.
 *
 * <p>ADMIN only. Price is a field-locked value; changing it through this use
 * case is the authorised internal path once the external importer is removed.
 */
public interface UpdateProductPriceUseCase {

    /** @param skuId    the ID of the SKU to update
     *  @param newPrice must be non-null and non-negative */
    void execute(Long skuId, Money newPrice);
}
