package tj.radolfa.application.ports.in.seller;

import tj.radolfa.application.readmodel.ProductCardDto;

/**
 * Use case: retrieve the full product card for a product the caller owns.
 * The service asserts seller ownership before returning — a seller cannot
 * fetch another seller's or a Radolfa-owned product via this use case.
 *
 * @throws tj.radolfa.domain.exception.ResourceNotFoundException if the product does not exist.
 * @throws tj.radolfa.domain.exception.FieldLockException        if the caller does not own the product.
 */
public interface GetMyProductCardUseCase {

    ProductCardDto execute(Long productBaseId, Long sellerId);
}
