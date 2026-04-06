package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.RestoreLoyaltyPointsUseCase;
import tj.radolfa.application.ports.in.order.CancelOrderUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

/**
 * Cancels an order and restores the reserved stock.
 *
 * <p>Rules:
 * <ul>
 *   <li>USER may only cancel their own PENDING orders.</li>
 *   <li>ADMIN may cancel any order that is not yet DELIVERED or already CANCELLED.</li>
 * </ul>
 */
@Service
public class CancelOrderService implements CancelOrderUseCase {

    private final LoadOrderPort                loadOrderPort;
    private final SaveOrderPort               saveOrderPort;
    private final LoadUserPort                loadUserPort;
    private final StockAdjustmentPort         stockAdjustmentPort;
    private final RestoreLoyaltyPointsUseCase restoreLoyaltyPointsUseCase;

    public CancelOrderService(LoadOrderPort loadOrderPort,
                              SaveOrderPort saveOrderPort,
                              LoadUserPort loadUserPort,
                              StockAdjustmentPort stockAdjustmentPort,
                              RestoreLoyaltyPointsUseCase restoreLoyaltyPointsUseCase) {
        this.loadOrderPort               = loadOrderPort;
        this.saveOrderPort               = saveOrderPort;
        this.loadUserPort                = loadUserPort;
        this.stockAdjustmentPort         = stockAdjustmentPort;
        this.restoreLoyaltyPointsUseCase = restoreLoyaltyPointsUseCase;
    }

    @Override
    @Transactional
    public void execute(Long orderId, Long requesterId, String reason) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        User requester = loadUserPort.loadById(requesterId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + requesterId));

        boolean isAdmin = requester.role() == UserRole.ADMIN;

        if (isAdmin) {
            if (order.status() == OrderStatus.DELIVERED
                    || order.status() == OrderStatus.CANCELLED) {
                throw new IllegalStateException(
                        "Cannot cancel an order in status: " + order.status());
            }
        } else {
            if (!order.userId().equals(requesterId)) {
                throw new IllegalStateException("Cannot cancel another user's order");
            }
            if (order.status() != OrderStatus.PENDING) {
                throw new IllegalStateException(
                        "Order can only be cancelled when PENDING, current status: " + order.status());
            }
        }

        // Restore stock for each item
        order.items().forEach(item -> {
            if (item.getSkuId() != null) {
                stockAdjustmentPort.increment(item.getSkuId(), item.getQuantity());
            }
        });

        // Restore loyalty points that were pessimistically deducted at checkout
        if (order.loyaltyPointsRedeemed() > 0) {
            restoreLoyaltyPointsUseCase.execute(order.userId(), order.loyaltyPointsRedeemed());
        }

        Order cancelled = new Order(order.id(), order.userId(), order.externalOrderId(),
                OrderStatus.CANCELLED, order.totalAmount(), order.items(), order.createdAt(),
                order.loyaltyPointsRedeemed(), order.loyaltyPointsAwarded());
        saveOrderPort.save(cancelled);
    }
}
