package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.SaveOrderItemPickStatePort;
import tj.radolfa.domain.exception.OrderItemAlreadyFullyPickedException;
import tj.radolfa.infrastructure.persistence.repository.OrderItemRepository;

@Component
public class OrderItemPickStateJpaAdapter implements SaveOrderItemPickStatePort {

    private final OrderItemRepository repository;

    public OrderItemPickStateJpaAdapter(OrderItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public int incrementPicked(Long orderItemId, Long actorUserId) {
        int updated = repository.incrementPicked(orderItemId, actorUserId);
        if (updated == 0) {
            int quantity = repository.findById(orderItemId)
                    .orElseThrow(() -> new IllegalArgumentException("OrderItem not found: " + orderItemId))
                    .getQuantity();
            throw new OrderItemAlreadyFullyPickedException(orderItemId, quantity);
        }
        return repository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("OrderItem not found: " + orderItemId))
                .getQuantityPicked();
    }
}
