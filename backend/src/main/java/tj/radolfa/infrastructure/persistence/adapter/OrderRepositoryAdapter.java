package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.infrastructure.persistence.mappers.OrderMapper;
import tj.radolfa.infrastructure.persistence.repository.OrderRepository;

import java.util.List;
import java.util.Optional;

@Component
public class OrderRepositoryAdapter implements LoadOrderPort, SaveOrderPort {

    private final OrderRepository repository;
    private final OrderMapper mapper;

    public OrderRepositoryAdapter(OrderRepository repository, OrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Order> loadByUserId(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toOrder)
                .toList();
    }

    @Override
    public Optional<Order> loadById(Long id) {
        return repository.findById(id)
                .map(mapper::toOrder);
    }

    @Override
    public Order save(Order order) {
        var entity = mapper.toEntity(order);
        // Ensure bidirectional relationship if needed,
        // usually MapStruct does it if configured, or manually:
        if (entity.getItems() != null) {
            entity.getItems().forEach(item -> item.setOrder(entity));
        }
        var saved = repository.save(entity);
        return mapper.toOrder(saved);
    }
}
