package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.InventoryTransaction;

import java.time.Instant;

public record InventoryTransactionDto(
        Long id,
        Long skuId,
        int delta,
        String type,
        String referenceType,
        Long referenceId,
        Long actorUserId,
        String notes,
        Instant occurredAt
) {
    public static InventoryTransactionDto from(InventoryTransaction tx) {
        return new InventoryTransactionDto(
                tx.id(),
                tx.skuId(),
                tx.delta(),
                tx.type().name(),
                tx.referenceType(),
                tx.referenceId(),
                tx.actorUserId(),
                tx.notes(),
                tx.occurredAt());
    }
}
