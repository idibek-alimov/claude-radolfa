package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.readmodel.PickSession;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.OrderStatus;

import java.util.List;

public record PickSessionDto(
        Long orderId,
        String externalOrderId,
        DeliveryType deliveryType,
        OrderStatus status,
        List<ItemRow> items
) {
    public record ItemRow(
            Long orderItemId,
            Long skuId,
            String skuCode,
            String barcode,
            String productName,
            String sizeLabel,
            int quantity,
            int quantityPicked
    ) {}

    public static PickSessionDto from(PickSession session) {
        return new PickSessionDto(
                session.orderId(),
                session.externalOrderId(),
                session.deliveryType(),
                session.status(),
                session.items().stream()
                        .map(i -> new ItemRow(
                                i.orderItemId(),
                                i.skuId(),
                                i.skuCode(),
                                i.barcode(),
                                i.productName(),
                                i.sizeLabel(),
                                i.quantity(),
                                i.quantityPicked()))
                        .toList());
    }
}
