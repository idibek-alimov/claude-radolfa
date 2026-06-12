package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.ActivateProductPort;
import tj.radolfa.infrastructure.persistence.repository.ProductBaseRepository;

@Component
public class ActivateProductAdapter implements ActivateProductPort {

    private final ProductBaseRepository productBaseRepository;

    public ActivateProductAdapter(ProductBaseRepository productBaseRepository) {
        this.productBaseRepository = productBaseRepository;
    }

    @Override
    public int activateIfAwaitingStock(Long productBaseId) {
        return productBaseRepository.activateIfAwaitingStock(productBaseId);
    }
}
