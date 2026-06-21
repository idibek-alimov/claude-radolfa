package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.GetWarehousePickQueueUseCase;
import tj.radolfa.application.ports.out.LoadAdminOrdersPort;
import tj.radolfa.application.readmodel.PickQueueItem;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetWarehousePickQueueService implements GetWarehousePickQueueUseCase {

    private final LoadAdminOrdersPort loadAdminOrdersPort;

    public GetWarehousePickQueueService(LoadAdminOrdersPort loadAdminOrdersPort) {
        this.loadAdminOrdersPort = loadAdminOrdersPort;
    }

    @Override
    public PageResult<PickQueueItem> execute(int page, int size, String search) {
        PageResult<LoadAdminOrdersPort.OrderRow> rows = loadAdminOrdersPort.search(
                search, List.of(OrderStatus.PAID, OrderStatus.AWAITING_COD), "createdAt", "ASC", page, size);

        List<PickQueueItem> items = rows.content().stream()
                .map(row -> {
                    Order order = row.order();
                    int totalUnits  = order.items().stream().mapToInt(OrderItem::getQuantity).sum();
                    int pickedUnits = order.items().stream().mapToInt(OrderItem::getQuantityPicked).sum();
                    return new PickQueueItem(
                            order.id(),
                            order.externalOrderId(),
                            order.deliveryType(),
                            order.createdAt(),
                            row.userName(),
                            totalUnits,
                            pickedUnits);
                })
                .toList();

        return new PageResult<>(items, rows.totalElements(), rows.number(), rows.size(), rows.last());
    }
}
