package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.ConfirmReturnedToWarehouseUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.exception.PickpointAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;

@Service
public class ConfirmReturnedToWarehouseService implements ConfirmReturnedToWarehouseUseCase {

    private final LoadOrderPort       loadOrderPort;
    private final SaveOrderPort       saveOrderPort;
    private final LoadUserPort        loadUserPort;
    private final StockAdjustmentPort stockAdjustmentPort;

    public ConfirmReturnedToWarehouseService(LoadOrderPort loadOrderPort,
                                              SaveOrderPort saveOrderPort,
                                              LoadUserPort loadUserPort,
                                              StockAdjustmentPort stockAdjustmentPort) {
        this.loadOrderPort       = loadOrderPort;
        this.saveOrderPort       = saveOrderPort;
        this.loadUserPort        = loadUserPort;
        this.stockAdjustmentPort = stockAdjustmentPort;
    }

    @Override
    @Transactional
    public void execute(Long orderId, Long staffUserId) {
        var order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.status() != OrderStatus.RETURN_INITIATED) {
            throw new IllegalStateException("Order is not RETURN_INITIATED: " + order.status());
        }

        var staff = loadUserPort.loadById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + staffUserId));

        if (staff.pickpointId() == null || !staff.pickpointId().equals(order.pickpointId())) {
            throw new PickpointAccessDeniedException(
                    "Staff " + staffUserId + " is not assigned to pickpoint " + order.pickpointId());
        }

        Order savedOrder = saveOrderPort.save(order.toBuilder()
                .status(OrderStatus.RETURNED_TO_WAREHOUSE)
                .returnedToWarehouseAt(Instant.now())
                .build());

        // Restore stock — package was never opened by the customer
        savedOrder.items().forEach(item -> {
            if (item.getSkuId() != null) {
                stockAdjustmentPort.increment(item.getSkuId(), item.getQuantity(),
                        InventoryTransactionType.RETURN_RESTORE,
                        "ORDER", savedOrder.id(), staffUserId);
            }
        });
    }
}
