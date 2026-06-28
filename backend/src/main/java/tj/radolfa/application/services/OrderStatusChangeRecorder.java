package tj.radolfa.application.services;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.SaveOrderStatusChangePort;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.OrderStatusChange;

import java.time.Instant;

/**
 * Centralised helper for appending an {@code order_status_changes} row.
 *
 * <p>Must be called inside an existing {@code @Transactional} boundary so the ledger
 * row and the status change are committed (or rolled back) together.
 */
@Component
public class OrderStatusChangeRecorder {

    private final SaveOrderStatusChangePort saveOrderStatusChangePort;

    public OrderStatusChangeRecorder(SaveOrderStatusChangePort saveOrderStatusChangePort) {
        this.saveOrderStatusChangePort = saveOrderStatusChangePort;
    }

    public void record(Long orderId, OrderStatus from, OrderStatus to,
                       Long actorUserId, String reason) {
        saveOrderStatusChangePort.append(
                new OrderStatusChange(null, orderId, from, to, actorUserId, reason, Instant.now()));
    }
}
