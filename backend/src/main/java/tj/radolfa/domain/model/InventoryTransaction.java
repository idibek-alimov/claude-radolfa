package tj.radolfa.domain.model;

import java.time.Instant;

public record InventoryTransaction(
        Long id,
        Long skuId,
        int delta,                 // positive = stock added, negative = stock removed, 0 = write-off
        InventoryTransactionType type,
        String referenceType,      // "ORDER", "CUSTOMER_RETURN", "STOCK_RECEIPT", "MANUAL", etc.
        Long referenceId,          // orderId, returnId, receiptId, or null for manual/system
        Long actorUserId,          // who triggered the change (null = system job)
        String notes,
        Instant occurredAt
) {}
