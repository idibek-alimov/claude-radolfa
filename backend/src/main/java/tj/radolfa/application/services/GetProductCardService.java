package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.GetProductCardUseCase;
import tj.radolfa.application.ports.out.LoadProductCardPort;
import tj.radolfa.application.readmodel.ProductCardDto;
import tj.radolfa.domain.exception.ResourceNotFoundException;

/**
 * Retrieves the full admin product card (ProductBase + all variants + SKUs).
 * Read-only; no mutations.
 */
@Service
public class GetProductCardService implements GetProductCardUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(GetProductCardService.class);

    private final LoadProductCardPort loadProductCardPort;

    public GetProductCardService(LoadProductCardPort loadProductCardPort) {
        this.loadProductCardPort = loadProductCardPort;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCardDto execute(Long productBaseId) {
        ProductCardDto card = loadProductCardPort.loadByProductBaseId(productBaseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: id=" + productBaseId));

        LOG.debug("[GET-PRODUCT-CARD] productBaseId={} variants={}", productBaseId, card.variants().size());
        return card;
    }
}
