package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.ReviewCustomerReturnItemsUseCase;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.RecordInventoryTransactionPort;
import tj.radolfa.application.ports.out.SaveCustomerReturnPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnItem;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.Resellability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReviewCustomerReturnItemsService implements ReviewCustomerReturnItemsUseCase {

    private final LoadCustomerReturnPort         loadCustomerReturnPort;
    private final SaveCustomerReturnPort         saveCustomerReturnPort;
    private final LoadOrderPort                  loadOrderPort;
    private final StockAdjustmentPort            stockAdjustmentPort;
    private final RecordInventoryTransactionPort recordInventoryTransactionPort;

    public ReviewCustomerReturnItemsService(LoadCustomerReturnPort loadCustomerReturnPort,
                                             SaveCustomerReturnPort saveCustomerReturnPort,
                                             LoadOrderPort loadOrderPort,
                                             StockAdjustmentPort stockAdjustmentPort,
                                             RecordInventoryTransactionPort recordInventoryTransactionPort) {
        this.loadCustomerReturnPort         = loadCustomerReturnPort;
        this.saveCustomerReturnPort         = saveCustomerReturnPort;
        this.loadOrderPort                  = loadOrderPort;
        this.stockAdjustmentPort            = stockAdjustmentPort;
        this.recordInventoryTransactionPort = recordInventoryTransactionPort;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        CustomerReturn cr = loadCustomerReturnPort.loadById(command.returnId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer return not found: " + command.returnId()));

        if (cr.getStatus() != CustomerReturnStatus.SENT_TO_WAREHOUSE) {
            throw new IllegalStateException(
                    "Review is only possible for returns in SENT_TO_WAREHOUSE status. Current: "
                    + cr.getStatus());
        }

        Map<Long, Resellability> reviewMap = command.reviews().stream()
                .collect(Collectors.toMap(ItemReview::orderItemId, ItemReview::resellability));

        Order order = loadOrderPort.loadById(cr.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + cr.getOrderId()));

        Map<Long, OrderItem> orderItemMap = order.items().stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));

        List<CustomerReturnItem> updatedItems = new ArrayList<>();

        for (CustomerReturnItem item : cr.getItems()) {
            Resellability r = reviewMap.get(item.orderItemId());
            Resellability finalResellability = r != null ? r : item.resellability();

            updatedItems.add(new CustomerReturnItem(item.id(), item.returnId(), item.orderItemId(),
                    item.quantity(), item.reason(), item.notes(), finalResellability));

            if (r == null) continue;

            OrderItem orderItem = orderItemMap.get(item.orderItemId());

            if (r == Resellability.RESELLABLE) {
                Long skuId = orderItem != null ? orderItem.getSkuId() : null;
                if (skuId != null) {
                    stockAdjustmentPort.increment(skuId, item.quantity(),
                            InventoryTransactionType.RETURN_RESTORE,
                            "CUSTOMER_RETURN", cr.getId(), command.adminUserId());
                }
            } else if (r == Resellability.DEFECTIVE) {
                Long skuId = orderItem != null ? orderItem.getSkuId() : null;
                recordInventoryTransactionPort.record(new InventoryTransaction(
                        null, skuId, 0, InventoryTransactionType.WRITE_OFF,
                        "CUSTOMER_RETURN", cr.getId(), command.adminUserId(),
                        null, Instant.now()));
            }
        }

        CustomerReturn updated = new CustomerReturn(
                cr.getId(), cr.getOrderId(), cr.getPickpointId(), cr.getReceivedByStaffId(),
                cr.getReceivedAt(), cr.getNotes(), updatedItems, cr.getStatus(),
                cr.getSentToWarehouseAt(), cr.getSentConfirmedByStaffId(),
                cr.getRefundApprovedAt(), cr.getRefundApprovedByAdminId(),
                cr.getGatewayRefundId(), cr.getRefundedAt());

        saveCustomerReturnPort.save(updated);
    }
}
