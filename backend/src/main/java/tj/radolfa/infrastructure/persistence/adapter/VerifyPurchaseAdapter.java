package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.VerifyPurchasePort;
import tj.radolfa.infrastructure.persistence.repository.OrderRepository;

@Component
public class VerifyPurchaseAdapter implements VerifyPurchasePort {

    private final OrderRepository orderRepository;

    public VerifyPurchaseAdapter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public boolean hasPurchasedVariant(Long userId, Long listingVariantId) {
        return orderRepository.hasPurchasedVariant(userId, listingVariantId);
    }
}
