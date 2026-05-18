package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadPickpointStatsPort;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.infrastructure.persistence.repository.CustomerReturnJpaRepository;
import tj.radolfa.infrastructure.persistence.repository.OrderRepository;

import java.time.Instant;
import java.util.List;

@Component
public class PickpointStatsJpaAdapter implements LoadPickpointStatsPort {

    private final OrderRepository              orderRepository;
    private final CustomerReturnJpaRepository  customerReturnJpaRepository;

    public PickpointStatsJpaAdapter(OrderRepository orderRepository,
                                    CustomerReturnJpaRepository customerReturnJpaRepository) {
        this.orderRepository             = orderRepository;
        this.customerReturnJpaRepository = customerReturnJpaRepository;
    }

    @Override
    public List<OrderCountRow> countOrdersByPickpointAndStatus(Instant overdueCutoff) {
        return orderRepository.countByPickpointAndStatus(overdueCutoff).stream()
                .map(row -> new OrderCountRow(
                        toLong(row[0]),
                        toLong(row[1]),
                        toLong(row[2]),
                        toLong(row[3]),
                        toLong(row[4])))
                .toList();
    }

    @Override
    public List<CustomerReturnCountRow> countCustomerReturnsReceived() {
        return customerReturnJpaRepository.countByStatusGroupByPickpoint(CustomerReturnStatus.RECEIVED).stream()
                .map(row -> new CustomerReturnCountRow(toLong(row[0]), toLong(row[1])))
                .toList();
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        return ((Number) value).longValue();
    }
}
