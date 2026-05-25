package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MyReturnDto(
        Long returnId,
        Long orderId,
        String status,
        Instant receivedAt,
        Instant sentToWarehouseAt,
        Instant refundedAt,
        BigDecimal totalRefundAmount,
        List<MyReturnItemDto> items
) {
    public record MyReturnItemDto(
            String productName,
            int quantity,
            BigDecimal refundAmount,
            String reason
    ) {}

    public static MyReturnDto from(CustomerReturn r, Order order) {
        List<MyReturnItemDto> items = r.getItems().stream()
                .map(i -> {
                    OrderItem oi = order.items().stream()
                            .filter(x -> x.getId().equals(i.orderItemId()))
                            .findFirst()
                            .orElse(null);
                    BigDecimal unitPrice = oi != null ? oi.getPrice().amount() : BigDecimal.ZERO;
                    BigDecimal refundAmt = unitPrice.multiply(BigDecimal.valueOf(i.quantity()));
                    String name = oi != null ? oi.getProductName() : "(unknown)";
                    return new MyReturnItemDto(name, i.quantity(), refundAmt, i.reason().name());
                })
                .toList();

        BigDecimal total = items.stream()
                .map(MyReturnItemDto::refundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MyReturnDto(r.getId(), r.getOrderId(), r.getStatus().name(),
                r.getReceivedAt(), r.getSentToWarehouseAt(), r.getRefundedAt(), total, items);
    }
}
