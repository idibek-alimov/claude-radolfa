package tj.radolfa.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * A walk-in customer return logged at a pickup point.
 *
 * <p>Tracks the full lifecycle of a physical return from staff receipt through
 * carrier handoff and eventual refund confirmation. A single order may produce
 * multiple CustomerReturn instances across separate visits (e.g. partial returns).
 *
 * <p>Pure Java — zero Spring / JPA / Jackson dependencies.
 */
public class CustomerReturn {

    private final Long                    id;
    private final Long                    orderId;
    private final Long                    pickpointId;
    private final Long                    receivedByStaffId;
    private final Instant                 receivedAt;
    private final String                  notes;
    private final List<CustomerReturnItem> items;

    private CustomerReturnStatus status;
    private Instant              sentToWarehouseAt;
    private Long                 sentConfirmedByStaffId;
    private Instant              refundApprovedAt;
    private Long                 refundApprovedByAdminId;
    private String               gatewayRefundId;
    private Instant              refundedAt;

    public CustomerReturn(Long id,
                          Long orderId,
                          Long pickpointId,
                          Long receivedByStaffId,
                          Instant receivedAt,
                          String notes,
                          List<CustomerReturnItem> items,
                          CustomerReturnStatus status,
                          Instant sentToWarehouseAt,
                          Long sentConfirmedByStaffId,
                          Instant refundApprovedAt,
                          Long refundApprovedByAdminId,
                          String gatewayRefundId,
                          Instant refundedAt) {
        this.id                     = id;
        this.orderId                = orderId;
        this.pickpointId            = pickpointId;
        this.receivedByStaffId      = receivedByStaffId;
        this.receivedAt             = receivedAt;
        this.notes                  = notes;
        this.items                  = items == null ? List.of() : Collections.unmodifiableList(items);
        this.status                 = status;
        this.sentToWarehouseAt      = sentToWarehouseAt;
        this.sentConfirmedByStaffId = sentConfirmedByStaffId;
        this.refundApprovedAt       = refundApprovedAt;
        this.refundApprovedByAdminId = refundApprovedByAdminId;
        this.gatewayRefundId        = gatewayRefundId;
        this.refundedAt             = refundedAt;
    }

    /** Staff confirms the 3PL carrier has collected this return batch. */
    public void markSentToWarehouse(Long staffId) {
        this.status                 = CustomerReturnStatus.SENT_TO_WAREHOUSE;
        this.sentToWarehouseAt      = Instant.now();
        this.sentConfirmedByStaffId = staffId;
    }

    /** Admin approves the refund; gateway call has been issued. */
    public void markRefundApproved(Long adminId, String gatewayRefundId) {
        this.status                  = CustomerReturnStatus.REFUND_APPROVED;
        this.refundApprovedAt        = Instant.now();
        this.refundApprovedByAdminId = adminId;
        this.gatewayRefundId         = gatewayRefundId;
    }

    /** Gateway confirmed the refund has been processed. */
    public void markRefunded() {
        this.status     = CustomerReturnStatus.REFUNDED;
        this.refundedAt = Instant.now();
    }

    public boolean isSent()     { return sentToWarehouseAt != null; }
    public boolean isRefunded() { return refundedAt != null; }

    // ---- Getters ----
    public Long                     getId()                     { return id; }
    public Long                     getOrderId()                { return orderId; }
    public Long                     getPickpointId()            { return pickpointId; }
    public Long                     getReceivedByStaffId()      { return receivedByStaffId; }
    public Instant                  getReceivedAt()             { return receivedAt; }
    public String                   getNotes()                  { return notes; }
    public List<CustomerReturnItem> getItems()                  { return items; }
    public CustomerReturnStatus     getStatus()                 { return status; }
    public Instant                  getSentToWarehouseAt()      { return sentToWarehouseAt; }
    public Long                     getSentConfirmedByStaffId() { return sentConfirmedByStaffId; }
    public Instant                  getRefundApprovedAt()       { return refundApprovedAt; }
    public Long                     getRefundApprovedByAdminId(){ return refundApprovedByAdminId; }
    public String                   getGatewayRefundId()        { return gatewayRefundId; }
    public Instant                  getRefundedAt()             { return refundedAt; }
}
