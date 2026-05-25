package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.RestoreLoyaltyPointsUseCase;
import tj.radolfa.application.ports.in.order.CancelOrderUseCase;
import tj.radolfa.application.ports.in.order.ExpireOrderUseCase;
import tj.radolfa.application.ports.out.DeliveryEventPublisher;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.util.Set;

import java.time.Instant;

/**
 * Cancels an order and restores the reserved stock.
 *
 * <p>Rules for user-driven cancellation:
 * <ul>
 *   <li>USER may only cancel their own PENDING orders.</li>
 *   <li>ADMIN may cancel any order that is not yet DELIVERED or already CANCELLED.</li>
 * </ul>
 *
 * <p>Also implements {@link ExpireOrderUseCase} for system-driven cancellations
 * (e.g. pickpoint storage expiry) that bypass the requester role check.
 */
@Service
public class CancelOrderService implements CancelOrderUseCase, ExpireOrderUseCase {

    private static final Set<OrderStatus> COURIER_ACTIVE = Set.of(
            OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERY_ATTEMPTED);

    private final LoadOrderPort                loadOrderPort;
    private final SaveOrderPort                saveOrderPort;
    private final LoadUserPort                 loadUserPort;
    private final StockAdjustmentPort          stockAdjustmentPort;
    private final RestoreLoyaltyPointsUseCase  restoreLoyaltyPointsUseCase;
    private final OrderNotificationService     orderNotificationService;
    private final DeliveryEventPublisher       deliveryEventPublisher;
    private final LoadCartPort                 loadCartPort;
    private final SaveCartPort                 saveCartPort;

    public CancelOrderService(LoadOrderPort loadOrderPort,
                              SaveOrderPort saveOrderPort,
                              LoadUserPort loadUserPort,
                              StockAdjustmentPort stockAdjustmentPort,
                              RestoreLoyaltyPointsUseCase restoreLoyaltyPointsUseCase,
                              OrderNotificationService orderNotificationService,
                              DeliveryEventPublisher deliveryEventPublisher,
                              LoadCartPort loadCartPort,
                              SaveCartPort saveCartPort) {
        this.loadOrderPort               = loadOrderPort;
        this.saveOrderPort               = saveOrderPort;
        this.loadUserPort                = loadUserPort;
        this.stockAdjustmentPort         = stockAdjustmentPort;
        this.restoreLoyaltyPointsUseCase = restoreLoyaltyPointsUseCase;
        this.orderNotificationService    = orderNotificationService;
        this.deliveryEventPublisher      = deliveryEventPublisher;
        this.loadCartPort                = loadCartPort;
        this.saveCartPort                = saveCartPort;
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
            Set<OrderStatus> requireRecall = Set.of(
                    OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.READY_FOR_PICKUP);
            if (requireRecall.contains(order.status())) {
                throw new IllegalStateException(
                        "Order in status " + order.status() + " cannot be cancelled directly. " +
                        "Use the Recall flow (POST /api/v1/admin/orders/{id}/request-recall) instead.");
            }
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

        cancelInternal(order, reason, requesterId);
    }

    @Override
    @Transactional
    public void execute(Long orderId, String reason) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.status() == OrderStatus.DELIVERED
                || order.status() == OrderStatus.CANCELLED
                || order.status() == OrderStatus.REFUNDED) {
            throw new IllegalStateException(
                    "Cannot expire an order in terminal status: " + order.status());
        }

        cancelInternal(order, reason, null);
    }

    private void cancelInternal(Order order, String reason, Long actorUserId) {
        order.items().forEach(item -> {
            if (item.getSkuId() != null) {
                stockAdjustmentPort.increment(item.getSkuId(), item.getQuantity(),
                        InventoryTransactionType.CANCELLATION, "ORDER", order.id(), actorUserId);
            }
        });

        if (order.loyaltyPointsRedeemed() > 0) {
            restoreLoyaltyPointsUseCase.execute(order.userId(), order.loyaltyPointsRedeemed());
        }

        Order cancelled = order.toBuilder()
                .status(OrderStatus.CANCELLED)
                .cancelledAt(Instant.now())
                .build();
        saveOrderPort.save(cancelled);
        orderNotificationService.notify(cancelled);

        // Release the cart so the user can re-checkout with the same items
        loadCartPort.findByPendingOrderId(order.id()).ifPresent(cart -> {
            cart.unlinkOrder();
            saveCartPort.save(cart);
        });

        // WebSocket push to affected field staff
        if (COURIER_ACTIVE.contains(order.status()) && order.courierId() != null) {
            deliveryEventPublisher.publishOrderCancelledToCourier(order.courierId(), order.id());
        } else if (order.status() == OrderStatus.READY_FOR_PICKUP && order.pickpointId() != null) {
            deliveryEventPublisher.publishOrderCancelledAtPickpoint(order.pickpointId(), order.id());
        }
    }
}
