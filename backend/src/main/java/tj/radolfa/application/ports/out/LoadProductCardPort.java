package tj.radolfa.application.ports.out;

import tj.radolfa.application.readmodel.ProductCardDto;

import java.util.Optional;

/**
 * Output port: load the full admin product card (base + all variants + SKUs) by product-base ID.
 */
public interface LoadProductCardPort {

    Optional<ProductCardDto> loadByProductBaseId(Long productBaseId);
}
