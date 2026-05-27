package tj.radolfa.application.readmodel;

import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PlacementView;

import java.util.List;

public record PickSession(
        Long orderId,
        String externalOrderId,
        DeliveryType deliveryType,
        OrderStatus status,
        List<Item> items
) {
    public record Item(
            Long orderItemId,
            Long skuId,
            String skuCode,
            String barcode,
            String productName,
            String sizeLabel,
            int quantity,
            int quantityPicked,
            List<PlacementView> placements
    ) {}
}
