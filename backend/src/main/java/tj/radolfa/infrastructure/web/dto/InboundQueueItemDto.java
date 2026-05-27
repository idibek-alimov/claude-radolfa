package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.readmodel.InboundQueueItem;

public record InboundQueueItemDto(
        Long   skuId,
        String skuCode,
        String barcode,
        String productName,
        int    unassignedQuantity
) {
    public static InboundQueueItemDto from(InboundQueueItem item) {
        return new InboundQueueItemDto(
                item.skuId(),
                item.skuCode(),
                item.barcode(),
                item.productName(),
                item.unassignedQuantity());
    }
}
