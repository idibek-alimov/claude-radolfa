package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.ScanOrderItemUnitUseCase;
import tj.radolfa.application.ports.in.order.UpdateOrderStatusUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.application.ports.out.RecordInventoryTransactionPort;
import tj.radolfa.application.ports.out.SaveOrderItemPickStatePort;
import tj.radolfa.domain.exception.BarcodeMismatchException;
import tj.radolfa.domain.exception.OrderItemAlreadyFullyPickedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.Sku;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ScanOrderItemUnitService implements ScanOrderItemUnitUseCase {

    private final LoadOrderPort loadOrderPort;
    private final LoadSkuPort loadSkuPort;
    private final SaveOrderItemPickStatePort saveOrderItemPickStatePort;
    private final RecordInventoryTransactionPort recordInventoryTransactionPort;
    private final LoadWarehousePort loadWarehousePort;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    public ScanOrderItemUnitService(LoadOrderPort loadOrderPort,
                                    LoadSkuPort loadSkuPort,
                                    SaveOrderItemPickStatePort saveOrderItemPickStatePort,
                                    RecordInventoryTransactionPort recordInventoryTransactionPort,
                                    LoadWarehousePort loadWarehousePort,
                                    UpdateOrderStatusUseCase updateOrderStatusUseCase) {
        this.loadOrderPort = loadOrderPort;
        this.loadSkuPort = loadSkuPort;
        this.saveOrderItemPickStatePort = saveOrderItemPickStatePort;
        this.recordInventoryTransactionPort = recordInventoryTransactionPort;
        this.loadWarehousePort = loadWarehousePort;
        this.updateOrderStatusUseCase = updateOrderStatusUseCase;
    }

    @Override
    public Result execute(Command cmd) {
        Order order = loadOrderPort.loadById(cmd.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + cmd.orderId()));

        if (order.status() != OrderStatus.PAID) {
            throw new IllegalArgumentException(
                    "Order " + cmd.orderId() + " is not in PAID status (current: " + order.status() + ")");
        }

        List<Long> skuIds = order.items().stream()
                .map(OrderItem::getSkuId)
                .filter(id -> id != null)
                .toList();
        Map<Long, Sku> skuMap = loadSkuPort.findAllByIdsAsMap(skuIds);

        String scannedBarcode = cmd.scannedBarcode().trim();
        List<OrderItem> matchingItems = order.items().stream()
                .filter(i -> {
                    Sku s = skuMap.get(i.getSkuId());
                    return s != null && scannedBarcode.equals(s.getBarcode());
                })
                .toList();

        if (matchingItems.isEmpty()) {
            throw new BarcodeMismatchException(scannedBarcode, cmd.orderId());
        }

        OrderItem matchedItem = matchingItems.stream()
                .filter(i -> !i.isFullyPicked())
                .findFirst()
                .orElseThrow(() -> {
                    OrderItem first = matchingItems.get(0);
                    return new OrderItemAlreadyFullyPickedException(first.getId(), first.getQuantity());
                });

        int newPickedQty = saveOrderItemPickStatePort.incrementPicked(matchedItem.getId(), cmd.actorUserId());

        Long warehouseId = loadWarehousePort.findDefault().id();
        recordInventoryTransactionPort.record(new InventoryTransaction(
                null,
                matchedItem.getSkuId(),
                warehouseId,
                0,
                InventoryTransactionType.PICK_VERIFICATION,
                "ORDER_ITEM",
                matchedItem.getId(),
                cmd.actorUserId(),
                null,
                Instant.now()));

        Order reloaded = loadOrderPort.loadById(cmd.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + cmd.orderId()));
        boolean orderFullyPicked = reloaded.items().stream().allMatch(OrderItem::isFullyPicked);
        if (orderFullyPicked) {
            updateOrderStatusUseCase.execute(new UpdateOrderStatusUseCase.Command(
                    cmd.orderId(), OrderStatus.PICKED, null, null, null));
        }

        return new Result(matchedItem.getId(), newPickedQty, matchedItem.getQuantity(), orderFullyPicked);
    }
}
