package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.order.GetCourierOrdersUseCase;
import tj.radolfa.application.ports.out.LoadCourierOrdersPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class GetCourierOrdersService implements GetCourierOrdersUseCase {

    private static final List<OrderStatus> ACTIVE_STATUSES = List.of(
            OrderStatus.DELIVERY_ATTEMPTED,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.SHIPPED);

    private static final Map<OrderStatus, Integer> STATUS_PRIORITY = Map.of(
            OrderStatus.DELIVERY_ATTEMPTED, 0,
            OrderStatus.OUT_FOR_DELIVERY,   1,
            OrderStatus.SHIPPED,            2);

    private final LoadCourierOrdersPort loadCourierOrdersPort;

    public GetCourierOrdersService(LoadCourierOrdersPort loadCourierOrdersPort) {
        this.loadCourierOrdersPort = loadCourierOrdersPort;
    }

    @Override
    public List<Order> execute(Long courierId) {
        return loadCourierOrdersPort.loadByCourierIdAndStatuses(courierId, ACTIVE_STATUSES)
                .stream()
                .sorted(Comparator
                        .comparingInt((Order o) -> STATUS_PRIORITY.getOrDefault(o.status(), 99))
                        .thenComparing(Order::createdAt))
                .toList();
    }
}
