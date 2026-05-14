package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.RefundOrderUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.UserRole;

import java.time.Instant;

/**
 * Marks an order as REFUNDED — a pure audit status transition.
 *
 * <p>No stock or loyalty side effects: those were handled at cancellation or delivery time.
 * The actual money-back is processed separately via the payment provider.
 */
@Slf4j
@Service
public class RefundOrderService implements RefundOrderUseCase {

    private final LoadOrderPort            loadOrderPort;
    private final SaveOrderPort            saveOrderPort;
    private final LoadUserPort             loadUserPort;
    private final OrderNotificationService orderNotificationService;

    public RefundOrderService(LoadOrderPort loadOrderPort,
                              SaveOrderPort saveOrderPort,
                              LoadUserPort loadUserPort,
                              OrderNotificationService orderNotificationService) {
        this.loadOrderPort            = loadOrderPort;
        this.saveOrderPort            = saveOrderPort;
        this.loadUserPort             = loadUserPort;
        this.orderNotificationService = orderNotificationService;
    }

    @Override
    @Transactional
    public void execute(Long orderId, Long requesterId, String reason) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        loadUserPort.loadById(requesterId)
                .filter(u -> u.role() == UserRole.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Only ADMIN may refund orders"));

        if (order.status() != OrderStatus.DELIVERED && order.status() != OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cannot refund an order in status: " + order.status()
                    + ". Only DELIVERED or CANCELLED orders may be refunded.");
        }

        log.info("Admin {} refunding order {} (reason: {})", requesterId, orderId, reason);

        Order refunded = new Order(
                order.id(), order.userId(), order.externalOrderId(),
                OrderStatus.REFUNDED, order.totalAmount(), order.items(), order.createdAt(),
                order.loyaltyPointsRedeemed(), order.loyaltyPointsAwarded(),
                order.deliveryType(), order.deliveryAddress(), order.preferredTimeWindow(), order.pickpointId(),
                order.courierName(), order.trackingNumber(), order.estimatedDeliveryDate(),
                order.shippedAt(), order.deliveredAt(), order.cancelledAt(), Instant.now());
        saveOrderPort.save(refunded);
        orderNotificationService.notify(refunded);
    }
}
