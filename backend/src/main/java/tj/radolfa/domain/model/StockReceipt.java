package tj.radolfa.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class StockReceipt {

    private final Long                  id;
    private final Long                  createdByUserId;
    private final Instant               createdAt;
    private final String                supplierReference;
    private final String                notes;
    private final List<StockReceiptItem> items;

    private StockReceiptStatus status;

    public StockReceipt(Long id,
                        Long createdByUserId,
                        Instant createdAt,
                        String supplierReference,
                        String notes,
                        StockReceiptStatus status,
                        List<StockReceiptItem> items) {
        this.id                = id;
        this.createdByUserId   = createdByUserId;
        this.createdAt         = createdAt;
        this.supplierReference = supplierReference;
        this.notes             = notes;
        this.status            = status;
        this.items             = items == null ? List.of() : Collections.unmodifiableList(items);
    }

    public void complete() {
        this.status = StockReceiptStatus.COMPLETED;
    }

    public Long                   getId()                { return id; }
    public Long                   getCreatedByUserId()   { return createdByUserId; }
    public Instant                getCreatedAt()         { return createdAt; }
    public String                 getSupplierReference() { return supplierReference; }
    public String                 getNotes()             { return notes; }
    public StockReceiptStatus     getStatus()            { return status; }
    public List<StockReceiptItem> getItems()             { return items; }
}
