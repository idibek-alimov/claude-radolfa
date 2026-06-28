package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadOrderStatusChangePort;
import tj.radolfa.application.ports.out.SaveOrderStatusChangePort;
import tj.radolfa.domain.model.OrderStatusChange;
import tj.radolfa.infrastructure.persistence.entity.OrderStatusChangeEntity;
import tj.radolfa.infrastructure.persistence.repository.OrderStatusChangeJpaRepository;

@Component
public class OrderStatusChangeJpaAdapter implements SaveOrderStatusChangePort, LoadOrderStatusChangePort {

    private final OrderStatusChangeJpaRepository repository;

    public OrderStatusChangeJpaAdapter(OrderStatusChangeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrderStatusChange append(OrderStatusChange change) {
        var entity = new OrderStatusChangeEntity(
                null,
                change.orderId(),
                change.statusFrom(),
                change.statusTo(),
                change.actorUserId(),
                change.reason(),
                change.occurredAt());
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Page<OrderStatusChange> findByOrderId(Long orderId, Pageable pageable) {
        return repository.findByOrderId(orderId, pageable).map(this::toDomain);
    }

    private OrderStatusChange toDomain(OrderStatusChangeEntity e) {
        return new OrderStatusChange(
                e.getId(),
                e.getOrderId(),
                e.getStatusFrom(),
                e.getStatusTo(),
                e.getActorUserId(),
                e.getReason(),
                e.getOccurredAt());
    }
}
