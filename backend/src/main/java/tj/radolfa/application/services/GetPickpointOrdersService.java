package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.order.GetPickpointOrdersUseCase;
import tj.radolfa.application.ports.out.LoadPickpointOrdersPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Service
public class GetPickpointOrdersService implements GetPickpointOrdersUseCase {

    private final LoadUserPort          loadUserPort;
    private final LoadPickpointOrdersPort loadPickpointOrdersPort;

    public GetPickpointOrdersService(LoadUserPort loadUserPort,
                                     LoadPickpointOrdersPort loadPickpointOrdersPort) {
        this.loadUserPort           = loadUserPort;
        this.loadPickpointOrdersPort = loadPickpointOrdersPort;
    }

    @Override
    public List<Order> execute(Long staffUserId) {
        var user = loadUserPort.loadById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + staffUserId));

        if (user.pickpointId() == null) {
            throw new IllegalStateException(
                    "PICKPOINT_STAFF user " + staffUserId + " has no pickpointId assigned");
        }

        List<Order> all = loadPickpointOrdersPort.loadByPickpointIdAndStatuses(
                user.pickpointId(), List.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.DELIVERED));

        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();

        return all.stream()
                .filter(o -> o.status() == OrderStatus.READY_FOR_PICKUP
                        || (o.status() == OrderStatus.DELIVERED
                                && o.deliveredAt() != null
                                && o.deliveredAt().isAfter(startOfToday)))
                .sorted(Comparator.comparingInt(o -> o.status() == OrderStatus.DELIVERED ? 1 : 0))
                .toList();
    }
}
