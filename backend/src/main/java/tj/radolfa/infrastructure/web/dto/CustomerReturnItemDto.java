package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Resellability;
import tj.radolfa.domain.model.ReturnReason;

public record CustomerReturnItemDto(
        Long orderItemId,
        String productName,
        String sizeLabel,
        String skuCode,
        int quantity,
        Money unitPrice,
        Money refundAmount,
        ReturnReason reason,
        String notes,
        Resellability resellability) {}
