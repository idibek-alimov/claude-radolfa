package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.StockReceipt;
import tj.radolfa.domain.model.StockReceiptItem;

import java.time.Instant;
import java.util.List;

public record StockReceiptDto(
        Long id,
        Long createdByUserId,
        Instant createdAt,
        String supplierReference,
        String notes,
        String status,
        List<StockReceiptItemDto> items,
        int totalUnitsReceived
) {
    public record StockReceiptItemDto(
            Long itemId,
            Long skuId,
            String skuCode,
            String productName,
            int quantityReceived,
            String notes
    ) {}

    public static StockReceiptDto from(StockReceipt r) {
        List<StockReceiptItemDto> items = r.getItems().stream()
                .map(i -> new StockReceiptItemDto(i.id(), i.skuId(), i.skuCode(),
                        i.productName(), i.quantityReceived(), i.notes()))
                .toList();
        int total = r.getItems().stream().mapToInt(StockReceiptItem::quantityReceived).sum();
        return new StockReceiptDto(r.getId(), r.getCreatedByUserId(), r.getCreatedAt(),
                r.getSupplierReference(), r.getNotes(), r.getStatus().name(), items, total);
    }
}
