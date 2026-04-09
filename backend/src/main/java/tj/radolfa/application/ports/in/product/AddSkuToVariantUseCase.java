package tj.radolfa.application.ports.in.product;

import tj.radolfa.domain.model.Money;

/**
 * In-Port: add a new SKU (size/price entry) to an existing listing variant.
 *
 * <p>Price and stock are ADMIN-managed fields — enforced at the controller level.
 */
public interface AddSkuToVariantUseCase {

    /**
     * Creates the new SKU and returns its generated primary key.
     */
    Long execute(Command command);

    record Command(
            Long   productBaseId,
            Long   variantId,
            String sizeLabel,
            Money  price,
            int    stockQuantity
    ) {}
}
