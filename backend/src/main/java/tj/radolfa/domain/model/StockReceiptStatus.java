package tj.radolfa.domain.model;

public enum StockReceiptStatus {
    DRAFT,     // started but not yet submitted (reserved for future multi-step flow)
    COMPLETED  // submitted; stock increments have been applied
}
