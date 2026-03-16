package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.infrastructure.persistence.entity.ProductVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.UserEntity;
import tj.radolfa.infrastructure.persistence.mappers.OrderMapper;
import tj.radolfa.infrastructure.persistence.repository.OrderRepository;

import java.util.List;
import java.util.Optional;

@Component
public class OrderRepositoryAdapter implements LoadOrderPort, SaveOrderPort {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final EntityManager em;

    public OrderRepositoryAdapter(OrderRepository repository,
                                  OrderMapper mapper,
                                  EntityManager em) {
        this.repository = repository;
        this.mapper = mapper;
        this.em = em;
    }

    @Override
    public List<Order> loadByUserId(Long userId) {
        return repository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toOrder)
                .toList();
    }

    @Override
    public Optional<Order> loadById(Long id) {
        return repository.findById(id)
                .map(mapper::toOrder);
    }

    @Override
    public Optional<Order> loadByErpOrderId(String erpOrderId) {
        return repository.findByErpOrderId(erpOrderId)
                .map(mapper::toOrder);
    }

    @Override
    public Order save(Order order) {
        var entity = mapper.toEntity(order);

        entity.setUser(em.getReference(UserEntity.class, order.userId()));

        if (entity.getItems() != null) {
            for (int i = 0; i < entity.getItems().size(); i++) {
                var item = entity.getItems().get(i);
                item.setOrder(entity);

                Long variantId = order.items().get(i).getVariantId();
                if (variantId != null) {
                    item.setVariant(em.getReference(ProductVariantEntity.class, variantId));
                }
            }
        }

        var saved = repository.save(entity);
        return mapper.toOrder(saved);
    }
}
