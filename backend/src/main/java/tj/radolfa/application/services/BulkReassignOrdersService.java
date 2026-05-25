package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.BulkReassignOrdersUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.util.Set;

@Service
public class BulkReassignOrdersService implements BulkReassignOrdersUseCase {

    private static final Set<OrderStatus> REASSIGNABLE = Set.of(
            OrderStatus.SHIPPED,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERY_ATTEMPTED);

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final LoadUserPort  loadUserPort;

    public BulkReassignOrdersService(LoadOrderPort loadOrderPort,
                                     SaveOrderPort saveOrderPort,
                                     LoadUserPort loadUserPort) {
        this.loadOrderPort = loadOrderPort;
        this.saveOrderPort = saveOrderPort;
        this.loadUserPort  = loadUserPort;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        User newCourier = loadUserPort.loadById(command.newCourierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Courier not found: " + command.newCourierId()));

        if (newCourier.role() != UserRole.COURIER || !newCourier.enabled()) {
            throw new IllegalStateException(
                    "User " + command.newCourierId() + " is not an active courier");
        }

        for (Long orderId : command.orderIds()) {
            Order order = loadOrderPort.loadById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

            if (!REASSIGNABLE.contains(order.status())) {
                throw new IllegalStateException(
                        "Order " + orderId + " cannot be reassigned from status " + order.status());
            }

            saveOrderPort.save(order.toBuilder().courierId(command.newCourierId()).build());
        }
    }
}
