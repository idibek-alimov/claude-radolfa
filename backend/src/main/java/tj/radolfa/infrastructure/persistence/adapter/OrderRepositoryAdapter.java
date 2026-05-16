package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadAdminOrdersPort;
import tj.radolfa.application.ports.out.LoadCourierOrderStatsPort;
import tj.radolfa.application.ports.out.LoadCourierOrdersPort;
import tj.radolfa.application.ports.out.LoadExpiringPickpointOrdersPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadPickpointOrdersPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.entity.OrderEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.entity.UserEntity;
import tj.radolfa.infrastructure.persistence.mappers.OrderMapper;
import tj.radolfa.infrastructure.persistence.repository.OrderRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrderRepositoryAdapter implements LoadOrderPort, SaveOrderPort, LoadAdminOrdersPort,
        LoadCourierOrdersPort, LoadPickpointOrdersPort, LoadCourierOrderStatsPort,
        LoadExpiringPickpointOrdersPort {

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
    public Optional<Order> loadByExternalOrderId(String externalOrderId) {
        return repository.findByExternalOrderId(externalOrderId)
                .map(mapper::toOrder);
    }

    @Override
    public List<Order> loadRecentPaidByUserId(Long userId, int limit) {
        return repository.findByUser_IdAndStatusOrderByCreatedAtDesc(
                        userId, OrderStatus.PAID, PageRequest.of(0, limit))
                .stream()
                .map(mapper::toOrder)
                .toList();
    }

    private static final Set<String> SORTABLE = Set.of("createdAt", "totalAmount", "status", "id");

    @Override
    public PageResult<OrderRow> search(String search, OrderStatus statusFilter,
                                       String sortBy, String sortDir,
                                       int page, int size) {
        String col = SORTABLE.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction dir = "ASC".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(dir, col));
        Page<OrderEntity> result = repository.findAll(
                OrderSpecifications.adminSearch(search, statusFilter), pageRequest);

        List<OrderRow> rows = result.getContent().stream()
                .map(e -> new OrderRow(
                        mapper.toOrder(e),
                        e.getUser().getPhone(),
                        e.getUser().getName()))
                .toList();

        return new PageResult<>(rows, result.getTotalElements(),
                page, size, result.isLast());
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity;
        if (order.id() != null) {
            entity = repository.findById(order.id())
                    .orElseThrow(() -> new IllegalStateException("Order not found: " + order.id()));
            mapper.updateEntity(order, entity);
        } else {
            entity = mapper.toEntity(order);
            entity.setUser(em.getReference(UserEntity.class, order.userId()));

            if (entity.getItems() != null) {
                for (int i = 0; i < entity.getItems().size(); i++) {
                    var item = entity.getItems().get(i);
                    item.setOrder(entity);

                    Long skuId = order.items().get(i).getSkuId();
                    if (skuId != null) {
                        item.setSku(em.getReference(SkuEntity.class, skuId));
                    }
                }
            }
        }

        var saved = repository.save(entity);
        return mapper.toOrder(saved);
    }

    @Override
    public List<Order> loadByCourierIdAndStatuses(Long courierId, List<OrderStatus> statuses) {
        return repository.findByCourierIdAndStatusInOrderByCreatedAtAsc(courierId, statuses)
                .stream().map(mapper::toOrder).toList();
    }

    @Override
    public List<Order> loadByPickpointIdAndStatuses(Long pickpointId, Collection<OrderStatus> statuses) {
        return repository.findByPickpointIdAndStatusInOrderByCreatedAtAsc(pickpointId, statuses)
                .stream().map(mapper::toOrder).toList();
    }

    @Override
    public List<Order> findReadyForPickupOlderThan(Instant cutoff) {
        return repository.findByStatusAndReadyForPickupAtLessThan(OrderStatus.READY_FOR_PICKUP, cutoff)
                .stream().map(mapper::toOrder).toList();
    }

    @Override
    public List<Order> findReadyForPickupInWindow(Instant startInclusive, Instant endExclusive) {
        return repository.findByStatusAndReadyForPickupAtBetween(
                        OrderStatus.READY_FOR_PICKUP, startInclusive, endExclusive)
                .stream().map(mapper::toOrder).toList();
    }

    @Override
    public Map<Long, CourierOrderStats> loadStats(Instant todayStart) {
        return repository.aggregateFleetStats(todayStart).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> new CourierOrderStats(
                                ((Number) row[1]).longValue(),
                                ((Number) row[2]).longValue(),
                                ((Number) row[3]).longValue())));
    }
}
