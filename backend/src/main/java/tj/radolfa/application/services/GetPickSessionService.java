package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.GetPickSessionUseCase;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.application.readmodel.PickSession;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.PlacementView;
import tj.radolfa.domain.model.Sku;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class GetPickSessionService implements GetPickSessionUseCase {

    private final LoadOrderPort          loadOrderPort;
    private final LoadSkuPort            loadSkuPort;
    private final InventoryPlacementPort placementPort;
    private final LoadWarehousePort      loadWarehousePort;

    public GetPickSessionService(LoadOrderPort loadOrderPort,
                                 LoadSkuPort loadSkuPort,
                                 InventoryPlacementPort placementPort,
                                 LoadWarehousePort loadWarehousePort) {
        this.loadOrderPort     = loadOrderPort;
        this.loadSkuPort       = loadSkuPort;
        this.placementPort     = placementPort;
        this.loadWarehousePort = loadWarehousePort;
    }

    @Override
    public PickSession execute(Long orderId) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        List<Long> skuIds = order.items().stream()
                .map(OrderItem::getSkuId)
                .filter(id -> id != null)
                .toList();
        Map<Long, Sku> skuMap = loadSkuPort.findAllByIdsAsMap(skuIds);

        Long wh = loadWarehousePort.findDefault().id();
        Map<Long, List<PlacementView>> views = placementPort.placementViewsForSkus(skuIds, wh);

        List<PickSession.Item> items = order.items().stream()
                .map(item -> {
                    Sku sku = skuMap.get(item.getSkuId());
                    return new PickSession.Item(
                            item.getId(),
                            item.getSkuId(),
                            item.getSkuCode(),
                            sku != null ? sku.getBarcode() : null,
                            item.getProductName(),
                            sku != null ? sku.getSizeLabel() : null,
                            item.getQuantity(),
                            item.getQuantityPicked(),
                            views.getOrDefault(item.getSkuId(), List.of()));
                })
                .toList();

        return new PickSession(
                order.id(),
                order.externalOrderId(),
                order.deliveryType(),
                order.status(),
                items);
    }
}
