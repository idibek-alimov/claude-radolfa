package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tj.radolfa.domain.model.CustomerReturnStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_returns")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class CustomerReturnEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "pickpoint_id", nullable = false)
    private Long pickpointId;

    @Column(name = "received_by_staff_id", nullable = false)
    private Long receivedByStaffId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CustomerReturnStatus status;

    @Column(name = "sent_to_warehouse_at")
    private Instant sentToWarehouseAt;

    @Column(name = "sent_confirmed_by_staff_id")
    private Long sentConfirmedByStaffId;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "refund_approved_at")
    private Instant refundApprovedAt;

    @Column(name = "refund_approved_by_admin_id")
    private Long refundApprovedByAdminId;

    @Column(name = "gateway_refund_id", length = 100)
    private String gatewayRefundId;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @OneToMany(mappedBy = "customerReturn", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CustomerReturnItemEntity> items = new ArrayList<>();
}
