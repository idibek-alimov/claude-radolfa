package tj.radolfa.domain.model;

public enum InventoryTransactionType {
    SALE,             // stock decremented at checkout
    CANCELLATION,     // stock restored on order cancel or expiry
    RECALL_RETURN,    // stock restored when a recalled order is physically returned
    RETURN_RESTORE,   // stock restored after warehouse resellability review (W3)
    WRITE_OFF,        // item deemed defective after review — paper trail only, no stock change
    RECEIPT,          // stock added via a Stock Receipt document (W2)
    MANUAL_ADJUSTMENT // admin sets absolute value or positive/negative delta
}
