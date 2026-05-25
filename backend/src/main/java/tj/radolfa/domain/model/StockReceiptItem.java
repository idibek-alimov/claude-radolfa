package tj.radolfa.domain.model;

public record StockReceiptItem(
        Long   id,
        Long   receiptId,
        Long   skuId,
        String skuCode,
        String productName,
        int    quantityReceived,
        String notes
) {}
