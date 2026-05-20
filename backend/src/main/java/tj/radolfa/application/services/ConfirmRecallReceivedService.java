package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.RestoreLoyaltyPointsUseCase;
import tj.radolfa.application.ports.in.order.ConfirmRecallReceivedUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.time.Instant;

@Service
public class ConfirmRecallReceivedService implements ConfirmRecallReceivedUseCase {

    private final LoadOrderPort              loadOrderPort;
    private final SaveOrderPort              saveOrderPort;
    private final LoadUserPort               loadUserPort;
    private final StockAdjustmentPort        stockAdjustmentPort;
    private final RestoreLoyaltyPointsUseCase restoreLoyaltyPointsUseCase;
    private final LoadCartPort               loadCartPort;
    private final SaveCartPort               saveCartPort;
    private final OrderNotificationService   orderNotificationService;

    public ConfirmRecallReceivedService(LoadOrderPort loadOrderPort,
                                         SaveOrderPort saveOrderPort,
                                         LoadUserPort loadUserPort,
                                         StockAdjustmentPort stockAdjustmentPort,
                                         RestoreLoyaltyPointsUseCase restoreLoyaltyPointsUseCase,
                                         LoadCartPort loadCartPort,
                                         SaveCartPort saveCartPort,
                                         OrderNotificationService orderNotificationService) {
        this.loadOrderPort               = loadOrderPort;
        this.saveOrderPort               = saveOrderPort;
        this.loadUserPort                = loadUserPort;
        this.stockAdjustmentPort         = stockAdjustmentPort;
        this.restoreLoyaltyPointsUseCase = restoreLoyaltyPointsUseCase;
        this.loadCartPort                = loadCartPort;
        this.saveCartPort                = saveCartPort;
        this.orderNotificationService    = orderNotificationService;
    }

    @Override
    @Transactional
    public void execute(Long orderId, Long actorUserId) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.status() != OrderStatus.RECALL_REQUESTED) {
            throw new IllegalStateException(
                    "Order is not RECALL_REQUESTED, cannot confirm recall. Status: " + order.status());
        }

        User actor = loadUserPort.loadById(actorUserId)
                .orElseThrow(() -> new IllegalStateException("Actor user not found: " + actorUserId));

        if (actor.role() == UserRole.COURIER) {
            if (!actorUserId.equals(order.courierId())) {
                throw new IllegalStateException(
                        "Courier " + actorUserId + " is not assigned to order " + orderId);
            }
        } else if (actor.role() == UserRole.PICKPOINT_STAFF) {
            if (actor.pickpointId() == null || !actor.pickpointId().equals(order.pickpointId())) {
                throw new IllegalStateException(
                        "Pickpoint staff " + actorUserId + " is not at the order's pickpoint");
            }
        }

        order.items().forEach(item -> {
            if (item.getSkuId() != null) {
                stockAdjustmentPort.increment(item.getSkuId(), item.getQuantity(),
                        InventoryTransactionType.RECALL_RETURN, "ORDER", order.id(), actorUserId);
            }
        });

        if (order.loyaltyPointsRedeemed() > 0) {
            restoreLoyaltyPointsUseCase.execute(order.userId(), order.loyaltyPointsRedeemed());
        }

        Order cancelled = order.toBuilder()
                .status(OrderStatus.CANCELLED)
                .cancelledAt(Instant.now())
                .recallConfirmedAt(Instant.now())
                .recallConfirmedByUserId(actorUserId)
                .build();
        saveOrderPort.save(cancelled);
        orderNotificationService.notify(cancelled);

        loadCartPort.findByPendingOrderId(orderId).ifPresent(cart -> {
            cart.unlinkOrder();
            saveCartPort.save(cart);
        });
    }
}
