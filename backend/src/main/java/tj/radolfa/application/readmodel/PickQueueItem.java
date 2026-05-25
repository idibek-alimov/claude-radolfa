package tj.radolfa.application.readmodel;

import tj.radolfa.domain.model.DeliveryType;

import java.time.Instant;

public record PickQueueItem(
        Long orderId,
        String externalOrderId,
        DeliveryType deliveryType,
        Instant createdAt,
        String customerName,
        int totalUnits,
        int pickedUnits
) {}
