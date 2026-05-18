package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.User;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record CustomerReturnDto(
        Long id,
        Long orderId,
        String customerName,
        String customerPhone,
        CustomerReturnStatus status,
        Instant receivedAt,
        Instant sentToWarehouseAt,
        List<CustomerReturnItemDto> items,
        String notes,
        Money totalRefundAmount) {

    public static CustomerReturnDto from(CustomerReturn r, Order order, User customer) {
        Map<Long, OrderItem> orderItemMap = order.items().stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));

        List<CustomerReturnItemDto> itemDtos = r.getItems().stream().map(ri -> {
            OrderItem oi = orderItemMap.get(ri.orderItemId());
            Money unitPrice   = oi != null ? oi.getPrice() : Money.ZERO;
            Money refundAmount = unitPrice.multiply(ri.quantity());
            String productName = oi != null ? oi.getProductName() : null;
            String skuCode     = oi != null ? oi.getSkuCode()     : null;
            return new CustomerReturnItemDto(
                    ri.orderItemId(), productName, skuCode,
                    ri.quantity(), unitPrice, refundAmount,
                    ri.reason(), ri.notes());
        }).toList();

        Money total = itemDtos.stream()
                .map(CustomerReturnItemDto::refundAmount)
                .reduce(Money.ZERO, Money::add);

        String name  = customer != null ? customer.name() : null;
        String phone = customer != null && customer.phone() != null ? customer.phone().value() : null;

        return new CustomerReturnDto(
                r.getId(), r.getOrderId(),
                name, phone,
                r.getStatus(),
                r.getReceivedAt(),
                r.getSentToWarehouseAt(),
                itemDtos,
                r.getNotes(),
                total);
    }
}
