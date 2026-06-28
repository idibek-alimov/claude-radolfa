package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.UpdateOrderStatusUseCase;
import tj.radolfa.application.ports.in.warehouse.CompletePickSessionUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;

@Service
@Transactional
public class CompletePickSessionService implements CompletePickSessionUseCase {

    private final LoadOrderPort loadOrderPort;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    public CompletePickSessionService(LoadOrderPort loadOrderPort,
                                      UpdateOrderStatusUseCase updateOrderStatusUseCase) {
        this.loadOrderPort = loadOrderPort;
        this.updateOrderStatusUseCase = updateOrderStatusUseCase;
    }

    @Override
    public void execute(Command cmd) {
        Order order = loadOrderPort.loadById(cmd.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + cmd.orderId()));

        if (!order.status().isPickable()) {
            throw new IllegalArgumentException(
                    "Order " + cmd.orderId() + " is not ready for picking (current: " + order.status() + ")");
        }

        boolean allPicked = order.items().stream().allMatch(OrderItem::isFullyPicked);
        if (!allPicked) {
            throw new IllegalArgumentException(
                    "Order " + cmd.orderId() + " is not fully picked — scan all units before completing");
        }

        updateOrderStatusUseCase.execute(
                new UpdateOrderStatusUseCase.Command(cmd.orderId(), OrderStatus.PICKED, null, null, null,
                        cmd.actorUserId()));
    }
}
