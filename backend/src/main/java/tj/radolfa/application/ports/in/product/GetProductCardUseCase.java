package tj.radolfa.application.ports.in.product;

import tj.radolfa.application.readmodel.ProductCardDto;
import tj.radolfa.domain.exception.ResourceNotFoundException;

/**
 * Use case: retrieve the full admin product card (base + all color variants + SKUs).
 *
 * <p>Throws {@link ResourceNotFoundException} when no product with the given ID exists.
 */
public interface GetProductCardUseCase {

    ProductCardDto execute(Long productBaseId);
}
