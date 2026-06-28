package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tj.radolfa.domain.model.PaymentStatus;

import java.time.Instant;

@Entity
@Table(name = "payment_status_changes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusChangeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_from", length = 32)
    private PaymentStatus statusFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_to", nullable = false, length = 32)
    private PaymentStatus statusTo;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @PrePersist
    void onPrePersist() {
        if (occurredAt == null) occurredAt = Instant.now();
    }
}
