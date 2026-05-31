package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.seller.GetMyProductCardUseCase;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadProductCardPort;
import tj.radolfa.application.readmodel.ProductCardDto;
import tj.radolfa.domain.exception.FieldLockException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ProductBase;

/**
 * Ownership-scoped product-card loader for seller use.
 *
 * <p>Loads the product base first (for the ownership check), then loads the
 * full card DTO. A seller may only retrieve products whose {@code seller_id}
 * equals their own — Radolfa-owned products ({@code seller_id IS NULL}) and
 * other sellers' products are rejected with {@link FieldLockException}.
 */
@Service
@Transactional(readOnly = true)
public class GetMyProductCardService implements GetMyProductCardUseCase {

    private final LoadProductBasePort loadProductBasePort;
    private final LoadProductCardPort loadProductCardPort;

    public GetMyProductCardService(LoadProductBasePort loadProductBasePort,
                                   LoadProductCardPort loadProductCardPort) {
        this.loadProductBasePort = loadProductBasePort;
        this.loadProductCardPort = loadProductCardPort;
    }

    @Override
    public ProductCardDto execute(Long productBaseId, Long sellerId) {
        ProductBase pb = loadProductBasePort.findById(productBaseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productBaseId));

        Long ownerSellerId = pb.getSellerId();
        // Radolfa-owned (null) or a different seller → deny
        if (ownerSellerId == null || !ownerSellerId.equals(sellerId)) {
            throw new FieldLockException("product",
                    "Seller may only view their own products");
        }

        return loadProductCardPort.loadByProductBaseId(productBaseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product card not found: " + productBaseId));
    }
}
