package tj.radolfa.application.ports.in.product;

import tj.radolfa.domain.model.Money;

/**
 * In-Port: add a new SKU to an existing listing variant.
 */
public interface AddSkuUseCase {

    Result execute(Command command);

    record Command(
            Long   variantId,
            String sizeLabel,
            Money  price,
            int    stockQuantity
    ) {}

    record Result(Long skuId, String skuCode) {}
}
