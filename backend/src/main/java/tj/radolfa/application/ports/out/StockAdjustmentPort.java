package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.InventoryTransactionType;

/**
 * Out-Port: adjust or set stock quantities on the persistence layer.
 *
 * <p>Separate from {@code SaveProductHierarchyPort} to allow lightweight
 * stock updates (e.g. after checkout) without loading the full hierarchy.
 *
 * <p>Each modifying call has a context-aware overload that records
 * an {@code InventoryTransaction} ledger entry in the same transaction.
 * Legacy single-arity methods still work (ledger entry recorded with null context).
 */
public interface StockAdjustmentPort {

    void decrement(Long skuId, int quantity);

    void increment(Long skuId, int quantity);

    void setAbsolute(Long skuId, int quantity);

    // ── Context-aware overloads (ledger-attributed) ───────────────────────────

    default void decrement(Long skuId, int quantity, Long orderId, Long actorUserId) {
        decrement(skuId, quantity);
    }

    default void increment(Long skuId, int quantity, InventoryTransactionType type,
                           String referenceType, Long referenceId, Long actorUserId) {
        increment(skuId, quantity);
    }
}
