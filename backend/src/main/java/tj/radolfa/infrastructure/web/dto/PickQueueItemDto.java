package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.readmodel.PickQueueItem;
import tj.radolfa.domain.model.DeliveryType;

import java.time.Instant;

public record PickQueueItemDto(
        Long orderId,
        String externalOrderId,
        DeliveryType deliveryType,
        Instant createdAt,
        String customerName,
        int totalUnits,
        int pickedUnits
) {
    public static PickQueueItemDto from(PickQueueItem item) {
        return new PickQueueItemDto(
                item.orderId(),
                item.externalOrderId(),
                item.deliveryType(),
                item.createdAt(),
                item.customerName(),
                item.totalUnits(),
                item.pickedUnits());
    }
}
