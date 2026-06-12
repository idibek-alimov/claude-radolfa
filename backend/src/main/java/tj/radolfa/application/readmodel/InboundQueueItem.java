package tj.radolfa.application.readmodel;

public record InboundQueueItem(
        Long   skuId,
        String skuCode,
        String barcode,
        String productName,
        int    unassignedQuantity
) {}
